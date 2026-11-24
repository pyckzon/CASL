"""Validación sintáctica local de los siete specs OpenAPI 3.1 del repositorio.

Postman CLI 1.74.0 `spec lint` requiere un token válido; este script permite
comprobar en local/CI los invariantes del contrato sin depender de ese token.
Requiere PyYAML (``python -m pip install pyyaml``).
"""

from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[2]
SPECS_DIR = ROOT / "postman" / "specs"
EXPECTED = {
    "users-v1.yaml",
    "catalog-v1.yaml",
    "cart-v1.yaml",
    "orders-v1.yaml",
    "payments-v1.yaml",
    "shipping-v1.yaml",
    "notification-v1.yaml",
}
LOCAL_REF = re.compile(r"^#/(.+)$")


def load(path: Path) -> dict[str, Any]:
    with path.open(encoding="utf-8") as handle:
        doc = yaml.safe_load(handle)
    if not isinstance(doc, dict):
        raise ValueError(f"{path.name}: la raíz no es un objeto YAML")
    return doc


def resolve_pointer(doc: Any, pointer: str) -> Any:
    current = doc
    for raw in pointer.split("/"):
        token = raw.replace("~1", "/").replace("~0", "~")
        if isinstance(current, dict) and token in current:
            current = current[token]
        elif isinstance(current, list) and token.isdigit() and int(token) < len(current):
            current = current[int(token)]
        else:
            raise ValueError(f"ref no resuelta: #/{pointer}")
    return current


def refs(value: Any) -> list[str]:
    found: list[str] = []
    if isinstance(value, dict):
        for key, item in value.items():
            if key == "$ref" and isinstance(item, str):
                found.append(item)
            else:
                found.extend(refs(item))
    elif isinstance(value, list):
        for item in value:
            found.extend(refs(item))
    return found


def uses_problem_json(doc: dict[str, Any]) -> bool:
    return "application/problem+json" in yaml.safe_dump(doc, allow_unicode=True)


def main() -> int:
    files = {p.name: p for p in SPECS_DIR.glob("*.yaml")}
    missing = EXPECTED - files.keys()
    if missing:
        print(f"Faltan specs: {sorted(missing)}")
        return 1

    errors: list[str] = []
    for name in sorted(EXPECTED):
        path = files[name]
        try:
            doc = load(path)
            if doc.get("openapi") != "3.1.0":
                errors.append(f"{name}: openapi debe ser 3.1.0")
            if not isinstance(doc.get("info"), dict) or not doc["info"].get("title"):
                errors.append(f"{name}: falta info.title")
            if not isinstance(doc.get("paths"), dict):
                errors.append(f"{name}: paths debe ser un objeto (puede estar vacío)")
            for ref in refs(doc):
                match = LOCAL_REF.match(ref)
                if match:
                    resolve_pointer(doc, match.group(1))
                    continue
                target_file, _, pointer = ref.partition("#")
                target = (path.parent / target_file).resolve()
                if not target.is_file():
                    errors.append(f"{name}: ref a archivo inexistente {ref}")
                    continue
                if pointer:
                    resolve_pointer(load(target), pointer.lstrip("/"))
            if not uses_problem_json(doc):
                errors.append(f"{name}: no describe application/problem+json")
            print(f"OK   {name}")
        except (OSError, ValueError, yaml.YAMLError) as exc:
            errors.append(f"{name}: {exc}")

    for error in errors:
        print(f"FAIL {error}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
