import argparse
import json
from pathlib import Path
from typing import Dict, Tuple

import tensorflow as tf


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Train posture classifier and export TFLite model")
    parser.add_argument("--data-dir", required=True, help="Dataset root with train/ and val/ subfolders")
    parser.add_argument("--img-size", type=int, default=160)
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--fine-tune-epochs", type=int, default=10)
    parser.add_argument("--learning-rate", type=float, default=5e-4)
    parser.add_argument("--fine-tune-lr", type=float, default=4.5e-5)
    parser.add_argument("--dropout", type=float, default=0.1)
    parser.add_argument("--dense-units", type=int, default=16)
    parser.add_argument("--grayscale", action="store_true", help="Convert RGB input to grayscale in pipeline")
    parser.add_argument("--alpha", type=float, default=0.35, help="MobileNetV2 alpha")
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument("--model-out", default="ml/EdgeImpulse/models/posture_model.keras")
    parser.add_argument("--tflite-out", default="ml/EdgeImpulse/models/tflite_learn_901615_40.tflite")
    parser.add_argument("--metrics-out", default="ml/EdgeImpulse/experiments/last_train_metrics.json")
    return parser.parse_args()


def build_datasets(args: argparse.Namespace):
    data_dir = Path(args.data_dir)
    train_dir = data_dir / "train"
    val_dir = data_dir / "val"

    if not train_dir.exists() or not val_dir.exists():
        raise FileNotFoundError("Expected dataset folders: <data-dir>/train and <data-dir>/val")

    input_shape = (args.img_size, args.img_size)

    train_ds = tf.keras.utils.image_dataset_from_directory(
        train_dir,
        label_mode="categorical",
        image_size=input_shape,
        batch_size=args.batch_size,
        seed=args.seed,
    )

    val_ds = tf.keras.utils.image_dataset_from_directory(
        val_dir,
        label_mode="categorical",
        image_size=input_shape,
        batch_size=args.batch_size,
        seed=args.seed,
        shuffle=False,
    )

    class_names = train_ds.class_names
    num_classes = len(class_names)
    if num_classes < 2:
        raise ValueError("At least two classes are required")

    aug = tf.keras.Sequential([
        tf.keras.layers.RandomFlip("horizontal"),
        tf.keras.layers.RandomZoom(0.12),
        tf.keras.layers.RandomBrightness(0.2),
    ])

    def preprocess(image, label):
        x = tf.cast(image, tf.float32)
        if args.grayscale:
            x = tf.image.rgb_to_grayscale(x)
            x = tf.image.grayscale_to_rgb(x)
        x = tf.keras.applications.mobilenet_v2.preprocess_input(x)
        return x, label

    autotune = tf.data.AUTOTUNE
    train_ds = train_ds.map(lambda x, y: (aug(x, training=True), y), num_parallel_calls=autotune)
    train_ds = train_ds.map(preprocess, num_parallel_calls=autotune).prefetch(autotune)
    val_ds = val_ds.map(preprocess, num_parallel_calls=autotune).prefetch(autotune)

    return train_ds, val_ds, class_names


def build_model(args: argparse.Namespace, num_classes: int) -> tf.keras.Model:
    input_tensor = tf.keras.Input(shape=(args.img_size, args.img_size, 3), name="image")

    base_model = tf.keras.applications.MobileNetV2(
        input_shape=(args.img_size, args.img_size, 3),
        include_top=False,
        weights="imagenet",
        alpha=args.alpha,
    )
    base_model.trainable = False

    x = base_model(input_tensor, training=False)
    x = tf.keras.layers.GlobalAveragePooling2D()(x)
    x = tf.keras.layers.Dense(args.dense_units, activation="relu")(x)
    x = tf.keras.layers.Dropout(args.dropout)(x)
    output = tf.keras.layers.Dense(num_classes, activation="softmax", name="probs")(x)

    model = tf.keras.Model(inputs=input_tensor, outputs=output)
    return model


def compile_model(model: tf.keras.Model, lr: float):
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=lr),
        loss="categorical_crossentropy",
        metrics=["accuracy"],
    )


def evaluate_to_dict(results: Dict[str, float], phase: str) -> Dict[str, float]:
    return {
        f"{phase}_loss": float(results[0]),
        f"{phase}_accuracy": float(results[1]),
    }


def export_tflite(model: tf.keras.Model, tflite_path: Path):
    tflite_path.parent.mkdir(parents=True, exist_ok=True)
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_model = converter.convert()
    tflite_path.write_bytes(tflite_model)


def main():
    args = parse_args()
    tf.keras.utils.set_random_seed(args.seed)

    train_ds, val_ds, class_names = build_datasets(args)
    model = build_model(args, num_classes=len(class_names))

    compile_model(model, args.learning_rate)
    model.fit(train_ds, validation_data=val_ds, epochs=args.epochs, verbose=2)

    base_eval = model.evaluate(val_ds, verbose=0)

    base_model = model.layers[1]
    base_model.trainable = True
    fine_tune_from = int(len(base_model.layers) * 0.35)
    for layer in base_model.layers[:fine_tune_from]:
        layer.trainable = False

    compile_model(model, args.fine_tune_lr)
    model.fit(train_ds, validation_data=val_ds, epochs=args.fine_tune_epochs, verbose=2)

    final_eval = model.evaluate(val_ds, verbose=0)

    model_out = Path(args.model_out)
    model_out.parent.mkdir(parents=True, exist_ok=True)
    model.save(model_out)

    export_tflite(model, Path(args.tflite_out))

    metrics = {
        "class_names": class_names,
        "config": vars(args),
        **evaluate_to_dict(base_eval, "base"),
        **evaluate_to_dict(final_eval, "final"),
    }

    metrics_out = Path(args.metrics_out)
    metrics_out.parent.mkdir(parents=True, exist_ok=True)
    metrics_out.write_text(json.dumps(metrics, indent=2), encoding="utf-8")

    print(json.dumps(metrics, indent=2))
    print(f"Model saved to: {model_out}")
    print(f"TFLite exported to: {args.tflite_out}")


if __name__ == "__main__":
    main()
