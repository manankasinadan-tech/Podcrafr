package com.example.data.model

data class PodcastTemplate(
    val title: String,
    val description: String,
    val category: String,
    val iconName: String,
    val script: String
)

object PodcastTemplates {
    val list = listOf(
        PodcastTemplate(
            title = "Tech & Futur de l'IA",
            description = "Débat passionné entre Clara et Alex sur les agents IA et l'avenir du travail.",
            category = "Technologie",
            iconName = "cpu",
            script = """Clara: [Souriante] Bienvenue dans 'L'Onde Tech' ! Aujourd'hui avec moi dans le studio : Alex, notre féru d'innovations.
Alex: [Enthousiaste] Salut Clara ! Salut à tous les auditeurs ! Préparez-vous parce qu'on va parler d'un sujet qui bouscule tout : les agents d'intelligence artificielle autonomes.
Clara: Exactement ! Et la grande question qu'on se pose tous : est-ce que nos routines quotidiennes vont être totalement métamorphosées d'ici 2 ans ?
Alex: [Rires] Totalement ! Enfin, Clara... avoue que l'idée d'avoir un assistant qui trie tes mails, prépare tes réunions et réserve tes vacances pendant que tu bois ton café, c'est le rêve absolu non ?
Marc: [Posé et réfléchi] Si je peux me permettre une nuance technique importante... Ce n'est pas seulement une question de confort. C'est un bouleversement dans la prise de décision humaine.
Sophie: [Vive et spontanée] Oh Marc, toujours là pour ramener de la gravité ! Mais tu as raison. Il faut garder notre sens critique tout en profitant du progrès.
Clara: [Chaleureuse] Bien dit Sophie ! Alors chers auditeurs, installez-vous confortablement, on décortique tout ça ensemble."""
        ),
        PodcastTemplate(
            title = "Le Mystère de l'Étoile Noire",
            description = "Une enquête captivante et atmosphérique contée par Thomas, Emma et Marc.",
            category = "Mystère & Histoire",
            iconName = "moon",
            script = """Thomas: [Voix profonde et mystérieuse] Minuit sonne sur la baie brumeuse. Une vieille balise scintille au loin dans la nuit silencieuse...
Emma: [Douce et contemplative] Tout a commencé un soir d'automne 1924, lorsqu'un journal intime a été retrouvé abandonné près du phare abandonné.
Marc: [Expert et méthodique] À l'intérieur : des coordonnées géographiques précises, et des équations astronomiques que la science de l'époque ne pouvait pas encore expliquer.
Thomas: [Percutant] Qui était cet astronome solitaire ? Et pourquoi les archives navales ont-elles été caviardées le lendemain même ?
Emma: [Chuchoté] C'est ce que nous allons tenter de comprendre au fil de cette nuit d'exploration."""
        ),
        PodcastTemplate(
            title = "Café Débat & Fous Rires",
            description = "Discussion légère et humaine sur les anecdotes improbables du quotidien.",
            category = "Comédie & Société",
            iconName = "coffee",
            script = """Alex: [Morte de rire] Non mais attendez, vous avez déjà essayé d'expliquer à vos grands-parents ce qu'est le cloud ?
Sophie: [Rires complices] Ah ! Ma grand-mère m'a sérieusement demandé si ses photos allaient être mouillées quand il pleut dehors !
Clara: [Amusée] Hahaha, c'est tellement mignon ! Et toi Marc, quelle est ta pire mésaventure avec la technologie moderne ?
Marc: [Ironique] Un jour, mon aspirateur robot a ouvert la porte du salon et s'est échappé sur le trottoir dans la rue. Mes voisins m'ont appelé en disant : Marc, ton robot promène le chien !
Sophie: [Éclats de rires] C'est pas vrai ! Hahaha ! Je veux absolument voir la vidéo de surveillance !
Alex: [Enthousiaste] Voilà pourquoi j'adore ce podcast ! Les amis, bienvenue dans notre dose quotidienne de bonne humeur !"""
        ),
        PodcastTemplate(
            title = "Aux Frontières de l'Univers",
            description = "Voyage au cœur des trous noirs et de l'astrophysique avec Emma, Thomas et Marc.",
            category = "Sciences",
            iconName = "planet",
            script = """Emma: [Inspirante] Fermez les yeux. À des millions d'années-lumière de notre Terre, la gravité s'effondre en un point d'une densité infinie.
Marc: [Passionné] Les trous noirs ne sont pas de simples monstres cosmiques. Ce sont de véritables laboratoires quantiques naturels.
Thomas: [Intense] Si vous vous approchiez de l'horizon des événements, le temps lui-même ralentirait pour vous par rapport au reste du cosmos.
Emma: [Émerveillée] Une seconde pour vous pourrait représenter un millénaire pour l'univers extérieur...
Marc: [Souriant] La relativité générale d'Einstein dans toute sa stupéfiante poésie."""
        )
    )
}
