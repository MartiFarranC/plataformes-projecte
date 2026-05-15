import argparse
import json
from pathlib import Path

import numpy as np


def parse_args():
    parser = argparse.ArgumentParser(
        description="Exporta embeddings del RAG a format NPZ compatible amb TensorFlow."
    )
    parser.add_argument(
        "--input",
        default="backend/knowledge/.rag-embeddings-cache.json",
        help="Ruta al fitxer de cache d'embeddings del backend."
    )
    parser.add_argument(
        "--output",
        default="ml/EdgeImpulse/models/rag_embeddings_tf.npz",
        help="Ruta de sortida del fitxer NPZ."
    )
    return parser.parse_args()


def main():
    args = parse_args()
    input_path = Path(args.input)
    output_path = Path(args.output)

    if not input_path.exists():
        raise FileNotFoundError(f"No existeix el fitxer d'entrada: {input_path}")

    payload = json.loads(input_path.read_text(encoding="utf-8"))
    documents = payload.get("documents", [])
    if not documents:
        raise ValueError("No hi ha documents amb embeddings al fitxer de cache.")

    ids = []
    sources = []
    hashes = []
    vectors = []

    for doc in documents:
        emb = doc.get("embedding")
        if not isinstance(emb, list) or len(emb) == 0:
            continue
        ids.append(doc.get("id", ""))
        sources.append(doc.get("source", ""))
        hashes.append(doc.get("textHash", ""))
        vectors.append(emb)

    if not vectors:
        raise ValueError("Cap embedding valid per exportar.")

    x_embeddings = np.asarray(vectors, dtype=np.float32)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    np.savez_compressed(
        output_path,
        embeddings=x_embeddings,
        ids=np.asarray(ids, dtype=object),
        sources=np.asarray(sources, dtype=object),
        text_hashes=np.asarray(hashes, dtype=object),
    )

    print(f"Exportacio completada: {output_path}")
    print(f"Num vectors: {x_embeddings.shape[0]}")
    print(f"Dimensio embedding: {x_embeddings.shape[1]}")


if __name__ == "__main__":
    main()
