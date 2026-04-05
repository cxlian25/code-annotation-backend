#!/usr/bin/env python3
import json
import sys


def tokenize(text: str):
    text = (text or "").lower().strip()
    if not text:
        return []

    tokens = []
    current = []
    for ch in text:
        if "\u4e00" <= ch <= "\u9fff":
            if current:
                tokens.append("".join(current))
                current = []
            tokens.append(ch)
        elif ch.isalnum() or ch == "_":
            current.append(ch)
        else:
            if current:
                tokens.append("".join(current))
                current = []

    if current:
        tokens.append("".join(current))

    return tokens


def ensure_wordnet():
    import nltk

    for pkg in ("wordnet", "omw-1.4"):
        try:
            nltk.data.find(f"corpora/{pkg}")
        except LookupError:
            nltk.download(pkg, quiet=True)


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "missing input path"}, ensure_ascii=False))
        return 2

    input_path = sys.argv[1]

    try:
        from nltk.translate.meteor_score import meteor_score
    except Exception as ex:
        print(json.dumps({"error": f"nltk import failed: {ex}", "python": sys.executable}, ensure_ascii=False))
        return 3

    try:
        with open(input_path, "r", encoding="utf-8-sig") as f:
            payload = json.load(f)
    except Exception as ex:
        print(json.dumps({"error": f"read input failed: {ex}"}, ensure_ascii=False))
        return 4

    references = payload.get("references", [])
    candidates = payload.get("candidates", [])
    total = min(len(references), len(candidates))

    if total == 0:
        print(json.dumps({"average": 0.0, "count": 0}, ensure_ascii=False))
        return 0

    try:
        ensure_wordnet()
    except Exception:
        # If WordNet download/check fails, we still try to run.
        pass

    scores = []
    for i in range(total):
        ref_tokens = tokenize(references[i])
        cand_tokens = tokenize(candidates[i])

        if not ref_tokens or not cand_tokens:
            scores.append(0.0)
            continue

        try:
            score = meteor_score([ref_tokens], cand_tokens)
        except Exception:
            score = 0.0
        scores.append(float(score))

    average = sum(scores) / len(scores)
    print(json.dumps({"average": average, "count": total}, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
