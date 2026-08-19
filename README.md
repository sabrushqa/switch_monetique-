# switch-monetique-service

Switch monétique LanaCash : autorisation des transactions TPE (canal ISO 8583
via jPOS) et e-commerce (REST), persistance Oracle.

## Prérequis

- **JDK 21** (obligatoire — voir ci-dessous, ne pas utiliser un JDK plus récent)
- Maven 3.9+
- Oracle accessible (voir `docker-compose.yml` à la racine du monorepo pour le
  service `oracle`)

### Pourquoi JDK 21 précisément

- `jPOS` 3.0.0 est compilé pour Java 22+ (class version 66) et n'est pas
  compatible avec ce projet → on reste sur `jpos` 2.1.9.
- À l'inverse, sur un JDK 22+ (par ex. le JDK 26 installé par défaut via
  Homebrew), **Mockito ne peut plus instrumenter certaines classes** et une
  partie des tests échoue avec `MockitoException: Could not modify all
  classes...` — une fausse alerte qui n'a rien à voir avec le code testé.

Un `maven-enforcer-plugin` est configuré dans le `pom.xml` : si vous lancez
Maven avec le mauvais JDK, le build échoue immédiatement avec un message
explicite au lieu de laisser les tests planter de façon trompeuse.

Si votre `mvn` par défaut ne pointe pas vers un JDK 21, forcez-le :

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test
```

(sur macOS avec Homebrew : `brew install openjdk@21` si besoin, puis la
commande ci-dessus le trouvera automatiquement).

## Lancer les tests

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test
```

29 tests (ISO 8583, filtre d'authentification interne, contrôleurs TPE et
e-commerce, signature monétique, autorisation) avec un seuil de couverture
JaCoCo à 80 % (lignes et branches), vérifié en phase `verify`.

## Build / run en conteneur

Le `Dockerfile` du service épingle déjà Java 21 des deux côtés (build
`maven:3.9.9-eclipse-temurin-21` et runtime `eclipse-temurin:21-jre-jammy`),
donc aucune action requise pour le déploiement — le problème de JDK ne
concerne que l'environnement de développement/CI local.

```bash
docker build -t switch-monetique-service .
```

Le service écoute sur le port `8090`. Toutes les routes `/api/switch/*`
exigent une signature HMAC (`X-Monetique-Signature`) ; le healthcheck du
conteneur se limite donc à vérifier que le port TCP accepte les connexions.
