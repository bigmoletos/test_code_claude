# 📚 Guide d'utilisation de Lombok et Annotations Personnalisées

## 🎯 Objectif

Réduire drastiquement le code boilerplate et améliorer la lisibilité grâce à Lombok et des annotations personnalisées pour le logging.

## ✨ Ce qui a été ajouté

### 1. **Annotations personnalisées**

#### `@Logged` - Configuration automatique du logger sur une classe

```java
@Logged(
    level = LogLevel.INFO,
    logFile = "./logs/my-service.log",
    maxFileSize = 10 * 1024 * 1024,  // 10MB
    maxBackupFiles = 5,
    compression = true,
    console = true
)
public class MyService {
    // Le logger est automatiquement configuré
}
```

#### `@LogExecution` - Logging automatique d'une méthode

```java
@LogExecution(
    level = LogLevel.DEBUG,
    logParams = true,        // Logger les paramètres d'entrée
    logResult = true,        // Logger le résultat
    logTime = true,          // Logger le temps d'exécution
    slowThreshold = 1000     // Warning si > 1 seconde
)
public String myMethod(String param) {
    // Votre code
    // Les logs sont automatiques !
    return "result";
}
```

### 2. **Aspect AOP - LoggingAspect**

Intercepte automatiquement les méthodes et ajoute :
- ✅ Log de l'entrée avec paramètres
- ✅ Log de la sortie avec résultat
- ✅ Mesure du temps d'exécution
- ✅ Détection des méthodes lentes
- ✅ Log des exceptions avec stack trace

## 📊 Comparaison Avant / Après

### ❌ Avant (verbose - ~40 lignes)

```java
@Service
@RequiredArgsConstructor
public class MyService {
    private static final CustomLogger logger = CustomLogger.getLogger("MyService");

    static {
        logger.setLevel(LogLevel.INFO)
              .setConsoleOutput(true)
              .setLogFile("./logs/my-service.log")
              .setMaxFileSize(10 * 1024 * 1024)
              .setMaxBackupFiles(5)
              .setCompressionEnabled(true);
    }

    private final MyRepository repository;

    public String processData(String input) {
        logger.info("Début de processData avec input: {}", input);
        long startTime = System.currentTimeMillis();

        try {
            // Votre logique métier
            String result = "processed: " + input;

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Fin de processData - Résultat: {}, Durée: {}ms", result, duration);

            if (duration > 1000) {
                logger.warn("Méthode lente détectée: processData a pris {}ms", duration);
            }

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("Erreur dans processData après {}ms", duration, e);
            throw e;
        }
    }
}
```

### ✅ Après (concis - ~15 lignes)

```java
@Service
@RequiredArgsConstructor
@Logged(
    level = LogLevel.INFO,
    logFile = "./logs/my-service.log",
    maxFileSize = 10 * 1024 * 1024,
    maxBackupFiles = 5,
    compression = true
)
public class MyService {
    private final MyRepository repository;

    @LogExecution(slowThreshold = 1000)
    public String processData(String input) {
        // Votre logique métier
        return "processed: " + input;
    }
}
```

**Réduction : 62% de code en moins !** 🎉

## 🔧 Configuration requise

### pom.xml

```xml
<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- AOP pour les aspects -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### application.yml

```yaml
spring:
  aop:
    auto: true  # Active AOP automatiquement
```

## 📁 Structure des fichiers créés

```
backend/src/main/java/com/sync/app/
├── annotation/
│   ├── Logged.java           # Annotation pour configuration du logger
│   └── LogExecution.java     # Annotation pour logging de méthodes
├── aspect/
│   └── LoggingAspect.java    # Aspect AOP pour le logging automatique
└── example/
    └── SimplifiedService.java # Exemple d'utilisation
```

## 🎓 Cas d'usage

### 1. Service simple avec logging automatique

```java
@Service
@Logged
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    @LogExecution
    public User findById(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}
```

**Résultat dans les logs :**
```
[DEBUG] → Entrée UserService.findById() avec paramètres: [1]
[DEBUG] ← Sortie UserService.findById() = User(id=1, name=John) (45ms)
```

### 2. Méthode critique avec détection de lenteur

```java
@LogExecution(slowThreshold = 500)
public void criticalOperation(String data) {
    // Si > 500ms, un WARNING est automatiquement émis
}
```

**Si lente :**
```
[WARN] Méthode lente détectée: MyService.criticalOperation() a pris 1234ms
```

### 3. Logging manuel pour cas spécifiques

```java
@Service
@Logged
public class ComplexService {
    private static final CustomLogger logger = CustomLogger.getLogger("ComplexService");

    public void complexOperation() {
        logger.info("Début d'opération complexe");

        // Étape 1
        logger.debug("Étape 1 terminée");

        // Étape 2
        logger.debug("Étape 2 terminée");

        logger.info("Opération complexe terminée");
    }
}
```

## 🎯 Services mis à jour

Les services suivants utilisent maintenant `@Logged` :

- ✅ `SyncTaskService` - Suppression de 7 lignes de configuration
- ✅ `SyncSchedulerService` - Suppression de 7 lignes de configuration
- ✅ `SyncTaskController` - Suppression de 7 lignes de configuration
- ✅ `SyncLogController` - Suppression de 7 lignes de configuration
- ⏳ `FileSyncService` - À venir (gardé avec config manuelle pour plus de contrôle)

**Total économisé : ~28 lignes de boilerplate supprimées !**

## 🚀 Avantages

1. **Code plus propre** : -60% de lignes pour la configuration du logging
2. **Maintenabilité** : Configuration centralisée via annotations
3. **Cohérence** : Même format de logs partout
4. **Performance** : Détection automatique des méthodes lentes
5. **Debugging** : Logs automatiques des exceptions
6. **Productivité** : Plus de temps sur la logique métier

## 📖 Pour aller plus loin

### Autres annotations Lombok utiles

```java
@Data                    // Génère getters, setters, toString, equals, hashCode
@Builder                 // Pattern Builder
@Slf4j                   // Logger SLF4J automatique
@AllArgsConstructor      // Constructeur avec tous les paramètres
@NoArgsConstructor       // Constructeur sans paramètres
@RequiredArgsConstructor // Constructeur avec les final/non-null uniquement
@Value                   // Classe immutable
@ToString                // Méthode toString()
@EqualsAndHashCode       // equals() et hashCode()
```

### Exemple complet

```java
@Service
@Logged(level = LogLevel.DEBUG)
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository repository;

    @LogExecution
    @Transactional
    public Product createProduct(@Valid ProductDto dto) {
        return repository.save(dto.toEntity());
    }
}
```

## 🎨 IDE Setup

### IntelliJ IDEA
1. Installer le plugin "Lombok"
2. Enable annotation processing:
   - Settings → Build → Compiler → Annotation Processors
   - ✅ Enable annotation processing

### VS Code
1. Installer l'extension "Lombok Annotations Support for VS Code"
2. Redémarrer l'IDE

---

**Note**: Cette approche combine le meilleur des deux mondes :
- Annotations pour la configuration déclarative
- Flexibilité pour les logs manuels quand nécessaire

