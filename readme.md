# Utilité :
1. Permet de générer les classes/enums entitées avec leurs dao interface et leur implication.
2. Modifie les accès à la BDD persistence unit, et la bdd name. 
3. (F) Copie les documents générés dans votre projet
# USAGE
## 1. Renseigner les entitées et les enums dans le fichier `entities.txt` sous le format :
```
c:nomClasse
e:nomEnum
c:nomClasse
c:nomClasse
e:nomEnum
```

## 2. Renseigner les données dans `bddConf.txt`, les données sont écrites en dur : 1e ligne et 2e ligne pour les deux données
```
persistence_units=pu_judo
database_name=JUDO_Database
```

## 3. (Facultatif) Renseigner le chemin vers votre projet InteliJ dans `inteliJProjectPath.txt`
Si le fichier est vide ou inexistant, aucune copie ne sera faite. Il est vide par défaut.
```
d:\Utilisateurs\user\Desktop\JavaAvance\TpBar
```


## /!\ dans `bddConf.txt` et `entities.txt` il y a des valeurs par défaut qu'il faut penser à modifier.

## Executer zzz.bat