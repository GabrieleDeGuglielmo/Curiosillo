# Curiosillo

App Android nativa di edutainment: curiosità quotidiane + quiz interattivo.

## Come aprire in Android Studio

1. Apri **Android Studio** (Hedgehog 2023.1.1+)
2. File > Open -> seleziona la cartella `Curiosillo/`
3. Aspetta il sync Gradle (scarica le dipendenze automaticamente)
4. Premi Run su un emulatore API 24+ o dispositivo fisico

## Struttura progetto

```
app/src/main/java/com/example/curiosillo/
  CuriosityApplication.kt   <- Application + DB init
  MainActivity.kt            <- NavHost con 3 rotte
  data/                      <- Room entities + DAO + Database
  repository/                <- Seed data + accesso dati
  viewmodel/                 <- CuriosityViewModel + QuizViewModel
  ui/
    HomeScreen.kt             <- Menu principale (2 pulsanti)
    CuriosityScreen.kt        <- Leggi curiosità + "Ho imparato!"
    QuizScreen.kt             <- Quiz con feedback colorato
    theme/                    <- Colori Material 3 + tema
```

## Logica quiz

Il quiz e disponibile solo dopo aver letto almeno una curiosità
e premuto "Ho imparato!". Le risposte vengono rimescolate ad ogni
sessione. Punteggio finale con percentuale di risposte corrette.

## Aggiungere nuove curiosità

Modifica `repository/CuriosityRepository.kt`, aggiungi alla lista
`curiosities` e al relativo `questions`. Il seed viene inserito
automaticamente al primo avvio su un database vuoto.
