# Notes de l'orateur : présenter GraphQL en 15 minutes

Ce document accompagne les slides de [`presentation-graphql.md`](presentation-graphql.md). Il s'adresse
à quelqu'un qui doit présenter le sujet **sans être expert de GraphQL**. Pour chaque slide, vous trouverez :

- 🎯 **Objectif** : ce que le public doit avoir compris à la fin de la slide ;
- 🗣️ **À dire** : un texte à dire à l'oral, à reformuler avec vos mots (ne le lisez pas) ;
- 💡 **Analogie / image** : pour rendre l'idée concrète ;
- ❓ **Questions probables** et réponses courtes ;
- ➡️ **Transition** vers la slide suivante.

Documents de référence pour réviser : [lexique](lexique-graphql.md),
[DataFetcher / DataLoader](dataloader-datafetcher.md), [diagramme du schéma GraphQL](schema-graphql.md),
[schéma de la base](schema-bdd.md).

---

## Avant la présentation

### Préparer la démo (la veille, pas 5 minutes avant)

1. Démarrer PostgreSQL et l'application :
   ```powershell
   docker compose up -d
   mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.docker.compose.enabled=false"
   ```
2. Ouvrir http://localhost:8080/graphiql et **préparer quatre onglets** contenant les requêtes q20, q24, m01 et e07,
   copiées depuis `requetes/requetes.http`. Pour m01, renseigner les variables dans le panneau *Variables*.
3. Ouvrir à côté la console où tournent les logs de l'application : c'est là que les requêtes SQL
   apparaissent pendant la démo du N+1.
4. Agrandir la police de GraphiQL et du terminal (Ctrl + molette) pour qu'elles soient lisibles depuis le fond de la salle.
5. **Plan B** : faire des captures d'écran des quatre résultats. Si la démo plante, vous les montrez sans perdre de temps.

> m01 crée un patient avec l'IPP `00020001`. Si vous la jouez deux fois, la seconde renvoie
> `IPP_DEJA_UTILISE`. Vous pouvez d'ailleurs vous en servir pour montrer une erreur métier.
> Pour repartir d'une base propre : `docker compose down -v` puis redémarrer.

### Les 3 idées que le public doit retenir

Si le public ne retient que trois choses, ce doivent être celles-ci. Revenez-y pendant toute la présentation :

1. **Le client choisit les champs, et la réponse a la forme de la requête.**
2. **Un schéma typé sert de contrat** entre le front et le back.
3. **Côté serveur, chaque champ a son résolveur**, et il faut regrouper les appels (N+1).

### Gestion du temps

| Slide | Repère horaire |
|---|---|
| 1–2 Titre, plan | 0:00 → 0:45 |
| 3–4 Le problème | 0:45 → 2:30 |
| 5–6 Le schéma | 2:30 → 5:30 |
| 7–8 Requêtes | 5:30 → 8:30 |
| 9–11 Serveur | 8:30 → 11:30 |
| 12–13 Erreurs, sécurité | 11:30 → 13:00 |
| 14 GraphQL ou REST | 13:00 → 13:30 |
| 15–16 Démo, à retenir | 13:30 → 15:00 |

Si vous êtes **en retard** à 8:30, passez vite sur la slide 11 (filtrage) et réduisez la démo à q20 et q24.
Si vous êtes **en avance**, passez plus de temps sur la démo : c'est ce que le public retient le mieux.

---

## Slide 1 : Titre

🎯 **Objectif** : poser le cadre et capter l'attention.

🗣️ **À dire**
> « Bonjour à tous. Pendant 15 minutes, je vais vous présenter GraphQL, une autre façon de concevoir
> une API que le REST que vous connaissez. L'idée de départ tient en une phrase : c'est le client qui demande
> exactement les données dont il a besoin, ni plus ni moins. Pour que ce soit concret, tous les exemples
> viennent d'un petit projet qu'on a construit : un mini système d'information hospitalier, avec des patients,
> leurs séjours à l'hôpital et leurs documents médicaux. »

💡 **Astuce** : présentez le domaine en une phrase. Il est volontairement simple. Le public n'a pas besoin
de connaître l'hôpital, il suffit de comprendre que « un patient a des séjours, un séjour a des documents ».

---

## Slide 2 : Plan

🎯 **Objectif** : rassurer en montrant que le chemin est balisé.

🗣️ **À dire**
> « On va partir d'un problème que vous avez tous rencontré en REST. Ensuite on verra le schéma, qui est
> le cœur de GraphQL, puis comment on écrit des requêtes, puis ce qui se passe côté serveur en Spring.
> On finira par les erreurs, la sécurité, et une petite démo en direct. »

Ne lisez pas le tableau, survolez-le. 20 secondes suffisent.

---

## Slide 3 : Le problème en REST

🎯 **Objectif** : que chacun reconnaisse un problème qu'il a déjà vécu. Sans problème, la solution n'a pas d'intérêt.

🗣️ **À dire**
> « Imaginez qu'on vous demande un écran "dossier patient" : le nom du patient, la liste de ses séjours,
> et pour chaque séjour ses documents. En REST, on appelle d'abord `/patients/1`, qui nous renvoie
> 25 champs alors qu'on en affiche 4. C'est la **sur-récupération** : on transporte des données pour rien.
> Ensuite on appelle `/patients/1/sejours`, puis pour chaque séjour `/sejours/12/documents`. C'est
> la **sous-récupération** : un seul appel ne suffit jamais, on enchaîne les allers-retours.
> Et le jour où le mobile veut un écran un peu différent, il faut un nouvel endpoint, un nouveau DTO,
> et le front attend que le back le livre. »

💡 **Analogie : le restaurant**
> « En REST, c'est un restaurant avec des **menus fixes** : vous voulez juste l'entrée et le dessert,
> on vous sert quand même le plat. Et si vous voulez un deuxième dessert, il faut repasser commande.
> GraphQL, c'est la **commande à la carte** : vous dites exactement ce que vous voulez, et tout arrive
> sur le même plateau. »

👉 **Interaction** : demandez « Qui a déjà écrit trois DTO différents pour la même entité ? ».
Des mains se lèvent presque toujours, et cela rend le problème concret.

📚 **Contexte historique** (si le temps le permet) : Facebook a créé GraphQL en 2012 parce que son application
mobile, sur des réseaux lents, faisait trop d'appels REST. Il est open source depuis 2015 et est maintenant géré par
la GraphQL Foundation. GitHub, Shopify et Netflix l'utilisent, entre autres.

➡️ **Transition** : « Voyons comment on écrit le même écran en GraphQL. »

---

## Slide 4 : La réponse GraphQL

🎯 **Objectif** : faire passer **l'idée principale de la présentation** : la réponse a la forme de la requête.

🗣️ **À dire**
> « Voici la même chose en GraphQL. On envoie une seule requête, qui décrit la forme des données voulues :
> le patient 1, son nom, ses séjours avec leur numéro, et pour chaque séjour les documents avec leur titre et
> leur statut. Regardez la réponse en dessous : c'est **exactement la même structure**. Les mêmes clés,
> imbriquées de la même façon. Rien de plus, rien de moins. »

Montrez la requête puis la réponse du doigt ou avec le pointeur, **ligne par ligne**. C'est le moment
où le public comprend.

> « Trois choses à retenir. Un : il n'y a qu'**une seule URL**, `/graphql`, et on lui envoie toujours un POST.
> Deux : c'est **le client** qui choisit les champs. Trois : **un seul aller-retour** pour tout l'écran. »

⚠️ **Idée reçue à corriger tout de suite**
> « Attention, malgré son nom, GraphQL n'est **pas une base de données** et ne remplace pas SQL.
> C'est une couche d'API, au même niveau que vos contrôleurs REST. Derrière, on a toujours notre base
> PostgreSQL et nos repositories. »

❓ **Questions probables**
- *« C'est forcément du POST ? »* → En pratique oui, c'est la convention. Le GET est possible pour les lectures,
  mais il est peu utilisé.
- *« Ça remplace REST ? »* → Non, c'est un autre outil. On en reparle à la slide 14.

➡️ **Transition** : « Mais comment le client sait-il quels champs il a le droit de demander ? Grâce au schéma. »

---

## Slide 5 : Le schéma, le contrat

🎯 **Objectif** : comprendre que le schéma est un **contrat typé** et la **source de vérité** partagée.

🗣️ **À dire**
> « Tout serveur GraphQL publie un **schéma**. C'est un fichier qui décrit tout ce qu'on peut demander,
> avec les types. On l'écrit dans un petit langage dédié qu'on appelle le SDL. Ici, le type `Patient` a
> un `id`, un `ipp`, un nom, un âge, et une liste de séjours. »

Expliquez les symboles un par un, c'est souvent ce qui bloque les débutants :

> « Le point d'exclamation veut dire **jamais null**. `String!`, c'est une chaîne toujours présente.
> Les crochets, c'est une **liste**. `[Sejour!]!` se lit : une liste jamais nulle, qui ne contient jamais
> de null, mais qui peut être vide. »

> « Remarquez le champ `sejours` : un patient pointe vers des séjours, qui pointent vers des documents,
> qui pointent eux-mêmes vers le patient. C'est un **graphe** d'objets, d'où le nom Graph-QL. »

> « Et le type `Query` est spécial : c'est le **point d'entrée**. Toute requête de lecture commence par
> un de ses champs, ici `patient` ou `patients`. »

💡 **Analogie** : le schéma, c'est la **carte du restaurant**. On ne peut commander que ce qui est sur la carte,
et la carte indique ce que contient chaque plat.

🗣️ **Pour finir**
> « Dans notre projet, on a écrit le schéma **en premier**, puis le code Java pour l'implémenter.
> On appelle ça *schema-first*. Le front et le back se mettent d'accord sur le schéma, et chacun peut
> ensuite travailler de son côté. »

📎 Pour une vue d'ensemble du schéma du projet, voir [`schema-graphql.md`](schema-graphql.md).
Vous pouvez la montrer si quelqu'un demande à quoi ressemble le schéma complet.

❓ **Questions probables**
- *« Les commentaires entre guillemets servent à quoi ? »* → Ce sont des **descriptions** : elles font partie
  du schéma et s'affichent dans les outils (GraphiQL). C'est une documentation intégrée.
- *« Pourquoi `patient` n'a pas de `!` ? »* → Parce qu'il renvoie `null` si le patient n'existe pas.
  C'est un choix de conception (requête q02).

---

## Slide 6 : Les briques du schéma

🎯 **Objectif** : donner le vocabulaire sans entrer dans le détail. Le public doit **reconnaître** ces mots,
pas les maîtriser.

🗣️ **À dire** : parcourez le tableau rapidement en donnant une phrase par ligne.

> « Un **type**, c'est un objet avec des champs. Un **scalaire**, c'est une valeur simple, une feuille de l'arbre :
> entier, texte, booléen. On peut créer nos propres scalaires : dans le projet, `IPP` est un scalaire qui
> n'accepte que 8 chiffres. Un **enum**, c'est une liste fermée de valeurs, comme le statut d'un document.
> Un **input**, c'est un objet qu'on envoie **en entrée**, typiquement pour créer un patient.
> Une **interface**, ce sont des champs communs à plusieurs types. Une **union**, c'est « soit l'un, soit l'autre » :
> une création de patient renvoie soit le patient, soit une erreur métier. Enfin, les **directives**
> sont des annotations avec un arobase, un peu comme en Java. »

> « Et il y a trois points d'entrée : **Query** pour lire, **Mutation** pour écrire, **Subscription** pour
> recevoir des événements en temps réel. »

💡 **Point fort à souligner** :
> « Le scalaire `IPP` est vérifié **avant** que notre code s'exécute. Si on envoie "ABC", la requête est
> rejetée tout de suite : notre code métier ne voit jamais une donnée mal formée. »

💡 **Correspondances avec Java** : type ≈ classe/record, enum ≈ enum Java, input ≈ DTO de requête,
interface ≈ interface Java, directive ≈ annotation.

➡️ **Transition** : « On a la carte. Voyons maintenant comment on passe commande. »

---

## Slide 7 : Écrire des requêtes

🎯 **Objectif** : montrer les quatre outils du quotidien : variables, alias, fragments, directives.

🗣️ **À dire**, en lisant la requête de haut en bas :

> « On donne un **nom** à la requête, `DossierPatient`. Ce n'est pas obligatoire, mais c'est pratique dans
> les logs. Elle déclare des **variables** avec un dollar : `$id` et `$avecDocs`. Leurs valeurs sont envoyées
> **à part**, dans le JSON en dessous. C'est comme une requête SQL préparée : on ne concatène jamais
> de valeurs dans le texte de la requête. »

> « `...Identite`, c'est un **fragment** : un bloc de champs qu'on définit une fois en bas et qu'on réutilise.
> C'est l'équivalent d'une méthode qu'on extrait pour ne pas se répéter. »

> « `lettres:` devant `documents`, c'est un **alias** : on renomme le champ dans la réponse. Ça permet de
> demander deux fois le même champ avec des arguments différents, par exemple les lettres d'un côté et
> tous les documents de l'autre. »

> « Et `@include(if: $avecDocs)` est une **directive** : on n'inclut les documents que si la variable vaut `true`.
> Le même texte de requête sert donc à deux usages. »

💡 **Point important** : chaque champ peut avoir des **arguments**, même un champ imbriqué comme
`documents(type: LETTRE_DE_LIAISON)`. C'est impossible à faire aussi simplement en REST.

📎 Pour aller plus loin, toutes ces notions sont illustrées une par une dans `requetes/requetes.http`
(requêtes q01 à q17).

❓ **Question probable** : *« Est-ce qu'on peut demander tous les champs, comme un `SELECT *` ? »*
→ **Non, volontairement.** Il faut toujours lister les champs voulus. C'est ce qui garantit que le client
ne reçoit que ce qu'il utilise, et que le serveur sait précisément ce qui est consommé.

---

## Slide 8 : Polymorphisme et mutations

🎯 **Objectif** : comprendre `__typename` et les fragments inline, et qu'une mutation renvoie un résultat typé.

🗣️ **À dire, première partie (recherche)**
> « La recherche globale renvoie des objets de types différents : des patients, des séjours, des documents.
> Comment le client sait-il ce qu'il reçoit ? Avec `__typename`, un champ spécial qui renvoie le nom du type.
> Et avec `... on Patient { }`, on dit : *si c'est un patient, donne-moi son nom ; si c'est un séjour, donne-moi
> son numéro*. C'est un **fragment inline**. »

💡 **Analogie** : c'est un `instanceof` suivi d'un cast en Java, ou un `switch` sur le type.

🗣️ **À dire, deuxième partie (mutation)**
> « Pour écrire, on utilise une **mutation**. Dans le projet, on nomme les mutations avec un **verbe métier** :
> `admettrePatient`, `muterPatient`, `sortirPatient`, plutôt que `updateSejour`. On lui passe un objet
> `entree` de type input. Et elle renvoie **soit** le séjour créé, **soit** une erreur métier. Le client teste
> `__typename` pour savoir lequel des deux il a reçu. »

> « Dernier détail : si on envoie plusieurs mutations dans la même requête, elles sont exécutées
> **l'une après l'autre**, dans l'ordre. Alors que les champs d'une query peuvent être résolus en parallèle. »

❓ **Question probable** : *« Pourquoi des verbes métier plutôt que du CRUD ? »* → Parce qu'admettre
un patient, ce n'est pas juste insérer une ligne : c'est créer un séjour, attribuer un numéro, enregistrer un
mouvement d'entrée, vérifier qu'il n'est pas déjà hospitalisé. Le nom reflète l'intention métier.

➡️ **Transition** : « Voilà pour le côté client. Passons de l'autre côté : comment on implémente tout ça en Spring ? »

---

## Slide 9 : Côté serveur, Spring for GraphQL

🎯 **Objectif** : relier GraphQL à ce que le public connaît (contrôleurs Spring, services, repositories).

🗣️ **À dire**
> « Côté serveur, chaque champ du schéma a une petite fonction qui sait calculer sa valeur. On l'appelle
> un **résolveur**, ou **DataFetcher** dans la bibliothèque Java. Bonne nouvelle : en Spring, on ne les écrit
> pas à la main, on utilise des annotations dans un `@Controller`, comme en REST. »

> « `@QueryMapping` relie une méthode à un champ de `Query`. Ici, la méthode `patient` répond au champ
> `patient`. L'argument GraphQL `id` arrive dans le paramètre grâce à `@Argument`, comme un `@PathVariable`. »

> « `@SchemaMapping(typeName = "Patient")` relie une méthode à un champ **de n'importe quel type**. Ici,
> le champ `age` du type `Patient`. La méthode reçoit le patient en paramètre et calcule l'âge.
> En fait, `@QueryMapping` n'est qu'un raccourci de `@SchemaMapping` pour le type `Query`. »

> « Et pour les champs simples comme `nom` ou `prenom` ? On n'écrit rien. Spring lit automatiquement
> le getter du même nom sur l'objet Java. On n'écrit du code que pour les points d'entrée, les champs calculés
> et les relations. »

💡 **Analogie clé pour un public CRUD**
> « Le DataFetcher, c'est votre méthode de `@RestController`, mais **à l'échelle d'un champ** au lieu
> d'une URL. Et le repository ne change pas du tout : le DataFetcher appelle le service, qui appelle
> le repository, exactement comme avant. »

📎 Explication détaillée : [`dataloader-datafetcher.md`](dataloader-datafetcher.md).

---

## Slide 10 : Le problème N+1

🎯 **Objectif** : c'est **le** piège technique de GraphQL. Le public doit comprendre le problème
et connaître le nom de la solution.

🗣️ **À dire**
> « Il y a un piège, et c'est le plus important de cette présentation côté serveur. Je demande 12 patients,
> et pour chacun son séjour en cours. Spring appelle la méthode qui charge les patients **une fois**,
> puis la méthode `sejourEnCours` **une fois par patient**. Si elle interroge la base, ça fait 12 requêtes SQL
> en plus de la première : 1 + N. Avec 100 patients, ça fait 101 requêtes. »

💡 **Analogie : les courses**
> « C'est comme faire les courses en revenant au magasin pour **chaque article** de la liste. Le DataLoader,
> c'est **prendre la liste complète** et tout acheter en un seul passage. »

🗣️ **La solution**
> « La solution s'appelle le **DataLoader**. Au lieu de charger tout de suite, chaque résolveur dit
> « j'aurai besoin des séjours du patient 3 ». Le DataLoader attend d'avoir toutes les demandes du même
> niveau, puis fait **une seule** requête : `WHERE patient_id IN (1, 2, …, 12)`. En Spring, il suffit de
> remplacer `@SchemaMapping` par **`@BatchMapping`** : la méthode reçoit la liste des 12 patients d'un coup,
> et renvoie une Map patient → séjours. »

> « Dans le projet, on a laissé exprès les deux versions côte à côte pour pouvoir comparer :
> `sejourEnCours` en N+1, et `sejours` en lot. On va le voir dans la démo, dans les logs SQL. »

📏 **Règle simple à donner** : « Toute relation entre entités → `@BatchMapping`. »

❓ **Questions probables**
- *« Pourquoi ne pas faire une jointure ? »* → Parce qu'on ne sait pas à l'avance ce que le client va demander.
  S'il ne demande pas les séjours, la jointure aurait été inutile. Le DataLoader ne charge que ce qui est demandé,
  et toujours en lot.
- *« Et JPA ? »* → C'est le même problème que le lazy loading en JPA, avec la même solution : charger en lot.

---

## Slide 11 : Comment les champs sont-ils filtrés ?

🎯 **Objectif** : comprendre que le serveur **ne construit que ce qui est demandé**, et connaître la limite de ce mécanisme.

🗣️ **À dire**
> « Une question revient souvent : le serveur charge-t-il tout, puis filtre-t-il ? Non. Le cycle se fait en
> quatre étapes. Un : on **analyse** le texte de la requête. Deux : on la **valide** contre le schéma. Si un
> champ n'existe pas ou si un IPP est mal formé, on s'arrête là et rien ne s'exécute. Trois : on **exécute**,
> et seuls les résolveurs des champs demandés sont appelés. Si vous ne demandez pas les séjours, la méthode
> qui charge les séjours n'est jamais appelée. Quatre : la **réponse** est construite à partir de la requête,
> pas à partir de l'objet Java. »

> « C'est l'inverse de REST avec Jackson, qui sérialise **tout** le DTO. »

⚠️ **La limite, à dire honnêtement**
> « En revanche, le repository charge toujours l'objet complet en base. Si je demande juste l'`id` du patient,
> on fait quand même un `SELECT *` sur la table patient. Le gain porte sur les **relations** et les **champs
> calculés** qu'on n'appelle pas, pas sur les colonnes. »

Si vous êtes en retard, résumez cette slide en une phrase : « Le serveur n'appelle que les résolveurs des
champs demandés. »

---

## Slide 12 : Les erreurs

🎯 **Objectif** : comprendre qu'en GraphQL, une réponse peut contenir **à la fois** des données et des erreurs,
et connaître la convention du projet.

🗣️ **À dire**
> « Premier choc pour ceux qui viennent du REST : en GraphQL, la réponse HTTP est presque toujours **200**,
> même en cas d'erreur. Les erreurs sont dans le corps de la réponse, dans un tableau `errors`. Et une réponse
> peut être **partielle** : ici, le patient est bien renvoyé, mais le champ `recherche` est en erreur parce que
> le texte était trop court. Il vaut `null`, et le tableau `errors` explique pourquoi, avec le chemin du champ. »

> « Dans le projet, on a fait un choix de conception : on distingue **deux familles** d'erreurs. Les erreurs
> techniques, ou les requêtes mal écrites, vont dans `errors`. Mais les **erreurs métier attendues**, comme
> « ce patient est déjà hospitalisé », sont renvoyées **comme des données**, grâce à l'union qu'on a vue :
> soit le séjour, soit une `ErreurMetier` avec un code. »

> « Pourquoi ? Parce que ces erreurs font partie du métier : le client doit les gérer comme un cas normal.
> Et comme elles sont typées dans le schéma, le client sait à l'avance lesquelles peuvent arriver. »

⚠️ **Le piège à bien montrer**
> « Si le client oublie de demander `... on ErreurMetier`, il reçoit un objet vide en cas d'erreur.
> Donc, toujours sélectionner ce fragment dans une mutation. »

💡 **Conseil** : le client doit s'appuyer sur le **code** (`SEJOUR_DEJA_EN_COURS`), qui est stable et
traduisible, et jamais sur le texte du message.

---

## Slide 13 : La sécurité

🎯 **Objectif** : comprendre que laisser le client écrire ses requêtes ouvre des risques, et qu'on les **borne**.

🗣️ **À dire**
> « On donne beaucoup de pouvoir au client : il écrit ses propres requêtes. Un client malveillant, ou
> simplement maladroit, pourrait écrire une requête énorme. Par exemple patient → séjours → patient → séjours…
> sur 50 niveaux. Ou demander 100 patients, avec 100 documents chacun, avec leur contenu complet. »

> « Dans le projet, on a quatre protections. Une **profondeur maximale** de 20 niveaux. Un **coût maximal** :
> chaque champ a un coût, défini avec une directive qu'on a créée, `@cout`. Une liste paginée multiplie le coût
> par la taille de la page, et au-delà de 5000, la requête est refusée. Une **pagination obligatoire** sur
> les listes, limitée à 100 éléments par page. Et les **scalaires** qui valident les données en entrée. »

> « Le point clé : tout ça est vérifié **avant** l'exécution. Une requête trop coûteuse ne touche jamais la base. »

💡 **Analogie** : c'est le **devis** avant les travaux. On estime le coût, et si ça dépasse le budget,
on ne commence pas.

❓ **Question probable** : *« Comment on choisit les coûts ? »* → C'est empirique : on donne un coût plus élevé
aux champs qui font une requête SQL ou un calcul lourd, et on multiplie par le nombre d'éléments
des listes. On ajuste ensuite en observant la production.

---

## Slide 14 : GraphQL ou REST ?

🎯 **Objectif** : rester **honnête et nuancé**. Le public doit repartir avec un critère de choix, pas avec
l'idée qu'il faut jeter REST.

🗣️ **À dire**
> « Alors, faut-il tout passer en GraphQL ? Non. GraphQL est très intéressant quand on a plusieurs clients
> aux besoins différents, comme un web et un mobile, quand les données sont très reliées entre elles, et
> quand un écran agrège beaucoup d'informations. Mais pour une API simple, du CRUD sur quelques ressources,
> REST est plus simple. REST profite aussi du cache HTTP natif, ce qui est plus compliqué en GraphQL
> puisque tout passe en POST sur une seule URL. Et pour télécharger un fichier, REST reste le bon outil. »

> « C'est un outil de plus dans la boîte, pas un remplaçant. »

---

## Slide 15 : Démo

🎯 **Objectif** : montrer que tout ce qui a été dit fonctionne vraiment. C'est souvent ce que le public retient le mieux.

**Déroulé conseillé (2 min)** :

1. **GraphiQL** (15 s)
   > « Voici GraphiQL, l'outil intégré. Regardez : quand je tape, il me propose les champs (*taper `pat`*).
   > Il les connaît parce qu'il lit le schéma. Et à droite, la documentation est générée automatiquement
   > à partir des descriptions du schéma. »

2. **q20, dossier patient** (30 s) : exécutez la requête.
   > « Une requête, tout le dossier : patient, séjours, mouvements, documents. Et la réponse a exactement
   > la forme de la requête. »
   Puis **supprimez un champ** de la requête et réexécutez : il disparaît de la réponse. C'est très parlant.

3. **q24, N+1** (40 s) : exécutez, puis passez sur les logs.
   > « Regardez les logs SQL : pour `sejourEnCours`, une requête par patient, on en voit 12 à la suite.
   > Pour `sejours`, une seule requête avec un `IN`. »

4. **m01, mutation** (20 s)
   > « Je crée un patient : je reçois un `Patient`. » Réexécutez : « Le même IPP une seconde fois :
   > je reçois une `ErreurMetier` avec le code `IPP_DEJA_UTILISE`, dans les données, pas dans `errors`. »

5. **e07, coût** (15 s)
   > « Une requête trop gourmande : refusée avant exécution, aucune requête SQL dans les logs. »

🆘 **Si la démo plante** : ne cherchez pas à corriger en direct. Dites « l'effet démo » avec le sourire,
puis montrez vos captures d'écran.

---

## Slide 16 : À retenir

🎯 **Objectif** : ancrer les messages clés et donner les ressources pour la suite.

🗣️ **À dire**
> « Pour résumer : une seule URL, un schéma typé, et c'est le client qui choisit les champs. La réponse
> a la forme de la requête. Côté Spring, on a `@QueryMapping`, `@SchemaMapping`, et surtout `@BatchMapping`
> pour éviter le N+1. Les erreurs métier sont dans les données, les erreurs techniques dans `errors`.
> Et on borne tout : profondeur, coût, pagination. »

> « Si vous voulez aller plus loin, tout est dans le dépôt : le lexique, la doc sur les DataLoaders,
> et surtout le fichier `requetes.http` avec 60 requêtes commentées à jouer vous-mêmes. Merci, avez-vous
> des questions ? »

---

## FAQ : questions qui reviennent souvent

| Question | Réponse courte |
|---|---|
| **GraphQL est plus lent que REST ?** | Pas intrinsèquement. Il y a un léger coût d'analyse, mais on économise des allers-retours. Le vrai risque de lenteur, c'est le N+1 mal géré. |
| **Comment on versionne une API GraphQL ?** | En général, on ne versionne pas. On **ajoute** des champs (sans casser les clients existants) et on marque les anciens `@deprecated` (voir q17, `Mouvement.ufCode`). |
| **Et le cache ?** | Le cache HTTP standard fonctionne mal (tout est en POST). On met du cache côté client (Apollo, urql), côté serveur (DataLoader, cache applicatif), ou on utilise des requêtes persistées en GET. |
| **Comment on gère l'authentification ?** | Comme en REST : un token dans l'en-tête HTTP, Spring Security devant `/graphql`. On peut ensuite contrôler l'accès **par champ** dans les résolveurs. |
| **Upload de fichiers ?** | Pas prévu par la spécification. On utilise souvent un endpoint REST à côté. |
| **Côté front, il faut une bibliothèque ?** | Non, un simple `fetch` en POST suffit. Mais des clients comme Apollo Client ou urql apportent le cache, la génération de types TypeScript, etc. |
| **Quelle différence entre Query et Mutation, techniquement ?** | La syntaxe est la même. Par convention, les mutations modifient les données, et leurs champs racines sont exécutés en série. |
| **On peut générer le code depuis le schéma ?** | Oui : des outils génèrent les types Java ou TypeScript à partir du schéma (GraphQL Code Generator, DGS codegen). |
| **Qui utilise GraphQL ?** | GitHub (API v4), Shopify, Netflix, Airbnb, PayPal, entre autres. |

---

## Erreurs fréquentes de l'orateur débutant

- **Trop de vocabulaire d'un coup** : à la slide 6, le public n'a pas besoin de tout retenir. Dites-le :
  « Pas besoin de tout mémoriser, vous retrouverez ces termes dans le lexique. »
- **Lire les slides** : les slides montrent le code, et c'est vous qui racontez l'histoire.
- **Passer trop vite sur la slide 4** : c'est l'idée centrale. Prenez le temps de montrer la correspondance
  entre la requête et la réponse.
- **Dénigrer REST** : beaucoup dans la salle font du REST tous les jours. Restez nuancé.
- **Improviser la démo** : préparez les onglets, et ayez un plan B.
