import argparse
import itertools
import json
import subprocess
import sys
from pathlib import Path
import shutil


def parse_args():
    parser = argparse.ArgumentParser(description="Run multiple ML experiments and select final model")
    parser.add_argument("--data-dir", required=True)
    parser.add_argument("--output-dir", default="ml/EdgeImpulse/experiments")
    parser.add_argument("--models-dir", default="ml/EdgeImpulse/models")
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--fine-tune-epochs", type=int, default=10)
    return parser.parse_args()


def run_command(cmd):
    print("Running:", " ".join(cmd))
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        raise RuntimeError(f"Command failed: {' '.join(cmd)}")
    return result.stdout


def main():
    args = parse_args()

    output_dir = Path(args.output_dir)
    models_dir = Path(args.models_dir)
    output_dir.mkdir(parents=True, exist_ok=True)
    models_dir.mkdir(parents=True, exist_ok=True)

    configs = [
        {"name": "baseline_rgb_96", "img_size": 96, "grayscale": False, "lr": 5e-4, "dropout": 0.1},
        {"name": "mid_rgb_128", "img_size": 128, "grayscale": False, "lr": 3e-4, "dropout": 0.15},
        {"name": "final_gray_160", "img_size": 160, "grayscale": True, "lr": 5e-4, "dropout": 0.1},
    ]

    alpha_options = [0.35, 0.5]
    dense_options = [16, 32]

    all_runs = []
    for cfg in configs:
        for alpha, dense_units in itertools.product(alpha_options, dense_options):
            run_id = f"{cfg['name']}_a{str(alpha).replace('.', '')}_d{dense_units}"
            metrics_out = output_dir / f"{run_id}.json"
            model_out = models_dir / f"{run_id}.keras"
            tflite_out = models_dir / f"{run_id}.tflite"

            cmd = [
                sys.executable,
                "ml/EdgeImpulse/scripts/train_model.py",
                "--data-dir", args.data_dir,
                "--img-size", str(cfg["img_size"]),
                "--batch-size", str(args.batch_size),
                "--epochs", str(args.epochs),
                "--fine-tune-epochs", str(args.fine_tune_epochs),
                "--learning-rate", str(cfg["lr"]),
                "--dropout", str(cfg["dropout"]),
                "--alpha", str(alpha),
                "--dense-units", str(dense_units),
                "--model-out", str(model_out),
                "--tflite-out", str(tflite_out),
                "--metrics-out", str(metrics_out),
            ]
            if cfg["grayscale"]:
                cmd.append("--grayscale")

            run_command(cmd)
            metrics = json.loads(metrics_out.read_text(encoding="utf-8"))
            metrics["run_id"] = run_id
            metrics["files"] = {
                "model": str(model_out),
                "tflite": str(tflite_out),
                "metrics": str(metrics_out),
            }
            all_runs.append(metrics)

    all_runs.sort(key=lambda x: x.get("final_accuracy", 0.0), reverse=True)
    best = all_runs[0]

    summary = {
        "total_runs": len(all_runs),
        "best_run": best,
        "ranking": [
            {
                "run_id": run["run_id"],
                "final_accuracy": run.get("final_accuracy"),
                "final_loss": run.get("final_loss"),
            }
            for run in all_runs
        ],
    }

    summary_path = output_dir / "experiments_summary.json"
    summary_path.write_text(json.dumps(summary, indent=2), encoding="utf-8")

    final_tflite = models_dir / "tflite_learn_901615_40.tflite"
    shutil.copy2(best["files"]["tflite"], final_tflite)

    print(json.dumps(summary, indent=2))
    print(f"Selected final model: {best['run_id']}")
    print(f"Final TFLite available at: {final_tflite}")


if __name__ == "__main__":
    main()
