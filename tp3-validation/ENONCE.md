# TP 3 — Validation automatique multi-critères : concevoir les agents

Durée : 1 h · Projet : `tp3-validation` · Java 21, Maven 3.9, OpenCode 2.x

## Objectif

Les tests fonctionnels disent qu'un code fait ce qu'on attend. Ils ne disent pas s'il est sûr, s'il tient la charge ni s'il est maintenable.

Dans ce TP, vous construisez vous-même une équipe d'agents qui évalue un code selon trois critères, rend un verdict, puis le fait corriger. **Aucun agent n'est fourni.** L'énoncé décrit ce que chaque agent doit accomplir et ce qu'on attend de l'ensemble ; il ne dit pas comment l'écrire. Le choix des prompts, des permissions et du format des échanges vous appartient.

À la fin, vous savez dire :
- ce que vos agents ont trouvé, et ce qu'ils ont manqué ;
- ce qui, dans votre équipe, est **garanti** par une permission et ce qui n'est que **demandé** dans un prompt.

## Le projet

Un module de facturation JDBC, en production depuis 2019, avec une base H2 en mémoire et 8 tests JUnit verts. Le code à évaluer est dans `src/main/java/fr/formation/facturation/legacy/`.

Ce code contient des défauts que les 8 tests ne détectent pas. Ils relèvent de trois critères : **sécurité**, **performance** et **maintenabilité**. L'énoncé ne les nomme pas : c'est à votre équipe d'agents de les trouver.

`AGENTS.md` donne les commandes du projet et trois conventions à respecter pendant toute correction : le texte des rapports ne change pas, les signatures publiques de `ServiceFacturation` non plus, et aucune dépendance n'est ajoutée.

## Ce que vous devez construire

Cinq agents, dans `.opencode/agents/`. Les noms sont imposés, pour que les résultats se comparent d'un binôme à l'autre.

| Agent | Sa mission | Ce qu'il rend | Ce qu'il ne doit pas pouvoir faire |
|---|---|---|---|
| `review-security` | Évaluer la sécurité du code, et rien d'autre | Une évaluation structurée (voir plus bas) | Modifier un fichier, lancer une commande |
| `review-perf` | Évaluer la performance du code, et rien d'autre | Une évaluation structurée | Modifier un fichier, lancer une commande |
| `review-maintainability` | Évaluer la maintenabilité du code, et rien d'autre | Une évaluation structurée | Modifier un fichier, lancer une commande |
| `validation` | Piloter la validation : faire évaluer, agréger, rendre un verdict, puis faire corriger | Un tableau de synthèse, un verdict global, la liste des corrections | Modifier un fichier ; lancer un autre agent que les quatre de ce TP |
| `developer` | Corriger le code de production à partir de constats précis | Le code corrigé et la preuve que les tests passent | Modifier les tests |

`validation` est l'agent avec lequel vous dialoguez. Les quatre autres ne sont lancés que par lui.

### Ce qu'on attend d'un reviewer

- **Un seul critère.** Un reviewer de sécurité qui commente le nommage des variables sort de son rôle.
- **Des constats vérifiables.** Chaque constat désigne un fichier et une ligne, explique le problème, propose une correction, et porte une gravité : `BLOQUANT`, `MAJEUR` ou `MINEUR`.
- **Une note de 1 à 5** sur son critère, dont l'échelle est définie (ce que vaut un 1, un 3, un 5).
- **Une sortie de même forme pour les trois reviewers**, qu'un programme pourrait lire sans l'interpréter. C'est à vous de définir cette forme.
- **Indépendance.** Un reviewer ne sait rien de ce qu'ont dit les deux autres.

### Ce qu'on attend de l'orchestration

1. Les trois évaluations sont lancées **en même temps**, pas l'une après l'autre.
2. `validation` applique une règle d'agrégation **écrite**, toujours la même :

   | Situation | Verdict global |
   |---|---|
   | Au moins un constat `BLOQUANT`, quel que soit le critère | `REFUS` |
   | Une note inférieure à 3 | `REFUS` |
   | Au moins deux constats `MAJEUR` au total | `À REVOIR` |
   | Aucun de ces cas | `ACCEPTÉ` |

3. `validation` affiche un tableau `critère | note | BLOQUANT | MAJEUR | verdict`, le verdict global et la liste des corrections triée par gravité, puis **s'arrête** : aucune correction n'est lancée sans votre accord.
4. Sur votre demande, `validation` confie les constats `BLOQUANT` et `MAJEUR` à `developer`, vérifie **lui-même** que les tests passent, puis relance les trois évaluations et rend un nouveau verdict.

## Déroulé

### Étape 0 — Mise en place (5 min)

```bash
cd ~/tp-ia-avance/tp-multi-agents-v4.0/tp3-validation-a-construire
mvn -B -ntp test 2>&1 | grep "Tests run:" | tail -1
git init -q && git add -A && git commit -qm "base" && git tag base
```

**Résultat attendu :** `Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`.

Les tests sont verts. Selon vous, ce code est-il bon pour la production ? Notez votre réponse, vous y reviendrez à la fin.

### Étape 1 — Témoin : une revue sans méthode (5 min)

Lancez `opencode`. Sur l'agent `build`, collez :

```
Fais une revue de code de src/main/java/fr/formation/facturation/legacy/.
Liste les problèmes que tu trouves. Ne modifie aucun fichier.
```

Notez les problèmes signalés et leur gravité. Gardez ce résultat : il sert de point de comparaison. Quittez OpenCode.

### Étape 2 — Concevoir et écrire les cinq agents (25 min)

Écrivez les cinq fichiers. Avant d'écrire un agent, répondez par écrit à trois questions : quelle est sa seule responsabilité ? que doit-il rendre, sous quelle forme ? que doit-il lui être **impossible** de faire ?

Contrôlez ensuite ce qu'OpenCode a réellement chargé :

```bash
opencode debug agents
git add -A && git commit -qm "agents de validation"
```

**Résultat attendu :** vos cinq agents apparaissent, `validation` en agent principal et les quatre autres en sous-agents, chacun avec les permissions que vous avez voulues.

### Étape 3 — Valider le code (10 min)

Lancez `opencode`, passez sur `validation` (touche Tab) et demandez-lui de valider le code de `src/main/`.

Observez : les trois évaluations partent-elles ensemble ? Ont-elles la même forme ? Le verdict global suit-il la règle d'agrégation ? `validation` s'arrête-t-il après le verdict ?

Un comportement qui ne correspond pas à ce que vous attendiez se corrige dans le fichier de l'agent, pas dans la conversation : quittez, corrigez, relancez.

### Étape 4 — Faire corriger, puis revalider (10 min)

Demandez à `validation` de faire corriger les constats `BLOQUANT` et `MAJEUR`, puis de revalider. Quittez OpenCode, puis :

```bash
git add -A && git commit -qm "corrections"
```

### Étape 5 — Confronter aux outils (5 min)

```bash
mvn -B -ntp test 2>&1 | grep "Tests run:" | tail -1
git diff --stat base -- src/test/
mvn -B -ntp -P qualite compile spotbugs:check pmd:check 2>&1 | grep -E "BUILD|High:|Medium:|PMD Failure"
```

SpotBugs cherche les failles de sécurité connues ; PMD mesure la complexité des méthodes. Comparez ce qu'ils signalent avec le dernier verdict de vos agents.

## Critères de réussite

**Les agents**
- [ ] `opencode debug agents` liste les cinq agents, avec les noms imposés.
- [ ] Un reviewer à qui l'on demande de modifier un fichier ou de lancer une commande en est empêché par OpenCode, pas par sa bonne volonté.
- [ ] `validation` ne peut modifier aucun fichier, et ne peut lancer aucun autre agent que les quatre de ce TP.
- [ ] `developer` ne peut pas modifier `src/test/`.

**L'orchestration**
- [ ] Les trois évaluations ont la même forme, avec une note, des constats localisés (fichier, ligne) et une gravité par constat.
- [ ] Le verdict global est celui que donne la règle d'agrégation, appliquée aux trois évaluations.
- [ ] Aucune correction n'a été lancée avant votre accord.

**Le résultat**
- [ ] `mvn -B -ntp test` : 8 tests verts.
- [ ] `git diff --stat base -- src/test/` n'affiche rien : aucun test n'a été modifié.
- [ ] `mvn -B -ntp -P qualite compile spotbugs:check pmd:check` se termine par `BUILD SUCCESS`.
- [ ] La dernière validation rend `ACCEPTÉ`.

Si les agents rendent `ACCEPTÉ` alors qu'un outil échoue encore, ne corrigez pas à la main : transmettez la sortie de l'outil à vos agents, et notez ce que cela dit de leur évaluation.

## Tableau de comparaison

| | Revue sans méthode (étape 1) | Vos reviewers (étape 3) | Outils (étape 5) |
|---|---|---|---|
| Problèmes de sécurité trouvés | | | |
| Problèmes de performance trouvés | | | |
| Problèmes de maintenabilité trouvés | | | |
| Remarques sans réel enjeu | | | |
| Résultat exploitable par un programme | oui / non | oui / non | oui / non |