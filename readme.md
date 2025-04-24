# Utilité :
1. Permet de générer les classes/enums entitées avec leurs dao interface et leur implication.
2. Modifie les accès à la BDD persistence unit, et la bdd name. 
3. Copie les documents générés dans votre projet InteliJ
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

## 3. Renseigner le chemin vers votre projet InteliJ dans `inteliJProjectPath.txt`
Si le fichier est vide ou inexistant, aucune copie ne sera faite. Il est vide par défaut. Et vous pourrez quand même récupérer le template généré sous le nom `tmp/`
```
D:\Utilisateurs\thomas.lemartinel\Desktop\JavaAvance\toast
```


## Se démerder pour compiler et executer `RunMain.java`