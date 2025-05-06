
# USAGE

## 0. Configuration dossier `config` (à effectuer une seule fois)
- `javapath.txt` : rensigner le chemin d'installation du dossier de votre JDK


## 1. Renseigner les entitées suivies de leurs attributs et les enums dans le fichier `entities.txt` sous le format :

```
c:Personnage
- String nom * -5 +10
- int age
- List<String> exemples 
e:nomEnum
- ENUM1
- ENUM2
c:nomClasse
c:nomClasse
e:nomEnum
```
"*" pour signifier l'unicité <br>
"+<int>" permet de metre une valeur maximale <br>
"-<int>" permet de metre une valeur minimale <br><br>
**/!\ Bien mettre un espace !**

## 2. Renseigner les données dans `projectSettings.txt`
**/!\ Ne pas mettre d'espaces**
```
database_name=JUDO_Database
inteliJ_project_path=D:\Utilisateurs\thomas.lemartinel\Desktop\JavaAvance\toast
```

### Clefs
- database_name : permet de configurer le application.properties
- inteliJ_project_path : **racine** du projet généré par inteliJ

*Obligatoire


## Features à venir 
- Faire un système de constances pour les messages d'erreur bean validation
- Ajouer une méthode "listAdd" pour tout les set, list, map...
- Ajouter un getter de collection avec un retour unModifiable