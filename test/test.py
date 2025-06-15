from flask import Flask, jsonify, request, abort
from flask_cors import CORS
from datetime import date

app = Flask(__name__)
CORS(app)

# ---------- Jeux d'essai ----------
soigneurs = [
    {"id": 1, "nom": "Alan Grant", "embauche": "2000-06-15"},
    {"id": 2, "nom": "Ellie Sattler", "embauche": "2001-02-20"},
    {"id": 3, "nom": "Robert Muldoon", "embauche": "1998-08-30"},
]

dinos = [
    {
        "id": 1,
        "nom": "Rexy",
        "female": True,
        "poids": 7000,
        "soigneur": [1, 2],  # List of soigneur ids
        "interventions": [{"key": 1, "value": 5}, {"key": 2, "value": 3}]  # Map<Soigneur, Integer> as list of dict
    },
    {
        "id": 2,
        "nom": "Blue",
        "female": False,
        "poids": 500,
        "soigneur": [3],
        "interventions": [{"key": 3, "value": 10}]
    }
]

visiteurs = [
    {
        "id": 1,
        "nom": "Tim Murphy",
        "dateVisite": "2023-06-15",
        "commentaires": ["Super visite !", "J'adore les raptors."]
    },
    {
        "id": 2,
        "nom": "Lex Murphy",
        "dateVisite": "2023-06-16",
        "commentaires": ["Impressionnant !"]
    }
]

# ---------- Utilitaires ----------
def get_next_id(lst):
    if lst: return max(x['id'] for x in lst) + 1
    return 1

def find_by_id(lst, id):
    for x in lst:
        if x['id'] == id:
            return x
    return None

# ---------- Routes génériques ----------
@app.route("/api/v1/dinos/", methods=["GET", "POST"])
def api_dinos():
    if request.method == "GET":
        return jsonify(dinos)
    data = request.json
    data["id"] = get_next_id(dinos)
    dinos.append(data)
    return jsonify(data), 201

@app.route("/api/v1/dinos/<int:id>/", methods=["GET", "PUT", "DELETE"])
def api_dino_detail(id):
    dino = find_by_id(dinos, id)
    if not dino:
        abort(404)
    if request.method == "GET":
        return jsonify(dino)
    if request.method == "PUT":
        newdata = request.json
        dino.update(newdata)
        return jsonify(dino)
    if request.method == "DELETE":
        dinos.remove(dino)
        return "", 204

@app.route("/api/v1/soigneurs/", methods=["GET", "POST"])
def api_soigneurs():
    if request.method == "GET":
        return jsonify(soigneurs)
    data = request.json
    data["id"] = get_next_id(soigneurs)
    soigneurs.append(data)
    return jsonify(data), 201

@app.route("/api/v1/soigneurs/<int:id>/", methods=["GET", "PUT", "DELETE"])
def api_soigneur_detail(id):
    soin = find_by_id(soigneurs, id)
    if not soin:
        abort(404)
    if request.method == "GET":
        return jsonify(soin)
    if request.method == "PUT":
        newdata = request.json
        soin.update(newdata)
        return jsonify(soin)
    if request.method == "DELETE":
        soigneurs.remove(soin)
        return "", 204

@app.route("/api/v1/visiteurs/", methods=["GET", "POST"])
def api_visiteurs():
    if request.method == "GET":
        return jsonify(visiteurs)
    data = request.json
    data["id"] = get_next_id(visiteurs)
    visiteurs.append(data)
    return jsonify(data), 201

@app.route("/api/v1/visiteurs/<int:id>/", methods=["GET", "PUT", "DELETE"])
def api_visiteur_detail(id):
    vis = find_by_id(visiteurs, id)
    if not vis:
        abort(404)
    if request.method == "GET":
        return jsonify(vis)
    if request.method == "PUT":
        newdata = request.json
        vis.update(newdata)
        return jsonify(vis)
    if request.method == "DELETE":
        visiteurs.remove(vis)
        return "", 204

# ---------- Démarrer le serveur ----------
if __name__ == "__main__":
    app.run(debug=True, port=8080)
