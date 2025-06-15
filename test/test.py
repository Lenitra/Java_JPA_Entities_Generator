from flask import Flask, jsonify, request
from flask_cors import CORS
from datetime import date

app = Flask(__name__)
CORS(app)

# ==== JEUX D'ESSAI ====

soigneurs = [
    {"id": 1, "nom": "Alice", "embauche": "2022-01-01"},
    {"id": 2, "nom": "Bob", "embauche": "2020-06-15"},
]

dinos = [
    {
        "id": 1,
        "nom": "Rex",
        "female": False,
        "poids": 1200,
        "soigneur": [1]
    },
    {
        "id": 2,
        "nom": "Blue",
        "female": True,
        "poids": 800,
        "soigneur": [2, 1]
    }
]

visiteurs = [
    {
        "id": 1,
        "nom": "Martin",
        "dateVisite": "2024-06-10",
        "commentaires": ["Super visite !", "Un peu bruyant..."]
    },
    {
        "id": 2,
        "nom": "Nina",
        "dateVisite": "2024-06-12",
        "commentaires": ["J'adore les dinos !"]
    }
]

def get_next_id(items):
    return max([i["id"] for i in items], default=0) + 1

# ==== ENDPOINTS DINO ====
@app.route("/api/v1/dinos/", methods=["GET"])
def get_dinos():
    return jsonify(dinos)

@app.route("/api/v1/dinos/", methods=["POST"])
def add_dino():
    data = request.json
    new_id = get_next_id(dinos)
    data["id"] = new_id
    # Par sécurité, on force soigneur à être une liste d'entiers
    data["soigneur"] = [int(x) for x in data.get("soigneur", [])]
    dinos.append(data)
    return jsonify(data), 201

@app.route("/api/v1/dinos/<int:id>/", methods=["PUT"])
def update_dino(id):
    data = request.json
    for d in dinos:
        if d["id"] == id:
            d.update(data)
            d["soigneur"] = [int(x) for x in d.get("soigneur", [])]
            return jsonify(d)
    return jsonify({"error": "Not found"}), 404

@app.route("/api/v1/dinos/<int:id>/", methods=["DELETE"])
def delete_dino(id):
    global dinos
    dinos = [d for d in dinos if d["id"] != id]
    return "", 204

# ==== ENDPOINTS SOIGNEUR ====
@app.route("/api/v1/soigneurs/", methods=["GET"])
def get_soigneurs():
    return jsonify(soigneurs)

@app.route("/api/v1/soigneurs/", methods=["POST"])
def add_soigneur():
    data = request.json
    new_id = get_next_id(soigneurs)
    data["id"] = new_id
    soigneurs.append(data)
    return jsonify(data), 201

@app.route("/api/v1/soigneurs/<int:id>/", methods=["PUT"])
def update_soigneur(id):
    data = request.json
    for s in soigneurs:
        if s["id"] == id:
            s.update(data)
            return jsonify(s)
    return jsonify({"error": "Not found"}), 404

@app.route("/api/v1/soigneurs/<int:id>/", methods=["DELETE"])
def delete_soigneur(id):
    global soigneurs
    soigneurs = [s for s in soigneurs if s["id"] != id]
    return "", 204

# ==== ENDPOINTS VISITEUR ====
@app.route("/api/v1/visiteurs/", methods=["GET"])
def get_visiteurs():
    return jsonify(visiteurs)

@app.route("/api/v1/visiteurs/", methods=["POST"])
def add_visiteur():
    data = request.json
    new_id = get_next_id(visiteurs)
    data["id"] = new_id
    # commentaires doit être une liste de string
    data["commentaires"] = data.get("commentaires", []) or []
    visiteurs.append(data)
    return jsonify(data), 201

@app.route("/api/v1/visiteurs/<int:id>/", methods=["PUT"])
def update_visiteur(id):
    data = request.json
    for v in visiteurs:
        if v["id"] == id:
            v.update(data)
            v["commentaires"] = v.get("commentaires", []) or []
            return jsonify(v)
    return jsonify({"error": "Not found"}), 404

@app.route("/api/v1/visiteurs/<int:id>/", methods=["DELETE"])
def delete_visiteur(id):
    global visiteurs
    visiteurs = [v for v in visiteurs if v["id"] != id]
    return "", 204

# ==== ENDPOINTS INDIVIDUELS (pour select option) ====
@app.route("/api/v1/soigneurs/<int:id>/", methods=["GET"])
def get_soigneur(id):
    soig = next((s for s in soigneurs if s["id"] == id), None)
    if soig:
        return jsonify(soig)
    return jsonify({"error": "Not found"}), 404

@app.route("/api/v1/dinos/<int:id>/", methods=["GET"])
def get_dino(id):
    dino = next((d for d in dinos if d["id"] == id), None)
    if dino:
        return jsonify(dino)
    return jsonify({"error": "Not found"}), 404

@app.route("/api/v1/visiteurs/<int:id>/", methods=["GET"])
def get_visiteur(id):
    vis = next((v for v in visiteurs if v["id"] == id), None)
    if vis:
        return jsonify(vis)
    return jsonify({"error": "Not found"}), 404

# ==== LANCEMENT ====
if __name__ == "__main__":
    app.run(debug=True, port=8080)
