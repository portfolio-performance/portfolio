#!/bin/bash
# ---------------------------------------------------------------
# Setzt den Autor der letzten 2 Commits auf stoeggich <stoeggich@gmail.com>
# Ausfuehren in Git Bash im Ordner C:\Users\Richard\git\portfolio
# ---------------------------------------------------------------

set -e

NAME="stoeggich"
EMAIL="stoeggich@gmail.com"
ANZAHL=2

echo "=============================================="
echo " Commit-Autor korrigieren"
echo "=============================================="
echo

# --- Sicherheitscheck: sind wir im richtigen Repo? ---
if [ ! -d .git ]; then
    echo "FEHLER: Hier ist kein Git-Repository."
    echo "Wechsle zuerst in den Projektordner:"
    echo "   cd  /c/Users/Richard/git/portfolio"
    exit 1
fi

if [ ! -f "name.abuchen.portfolio/src/name/abuchen/portfolio/datatransfer/pdf/SBrokerPDFExtractor.java" ]; then
    echo "FEHLER: Das sieht nicht nach dem Portfolio-Performance-Repo aus."
    echo "Aktueller Ordner: $(pwd)"
    exit 1
fi

# --- Sicherheitscheck: keine uncommitteten Aenderungen ---
if [ -n "$(git status --porcelain)" ]; then
    echo "FEHLER: Es gibt uncommittete Aenderungen."
    echo "Committe oder stashe sie zuerst:"
    echo "    git stash"
    exit 1
fi

# --- Sicherheitscheck: sind es wirklich nur 2 eigene Commits? ---
echo "Die letzten $ANZAHL Commits, die umgeschrieben werden:"
echo "----------------------------------------------"
git log -$ANZAHL --format="  %h  %an <%ae>%n      %s"
echo "----------------------------------------------"
echo
echo "Der Commit DAVOR bleibt unveraendert (Upstream):"
git log -1 --skip=$ANZAHL --format="  %h  %an <%ae>%n      %s"
echo
read -p "Passt das? Nur diese $ANZAHL Commits gehoeren Dir? [j/N] " ok
if [ "$ok" != "j" ] && [ "$ok" != "J" ]; then
    echo "Abgebrochen. Nichts geaendert."
    exit 0
fi

# --- Autor setzen (nur fuer dieses Repo) ---
git config user.name "$NAME"
git config user.email "$EMAIL"
echo
echo "Autor gesetzt: $NAME <$EMAIL>"

# --- Commits neu schreiben ---
echo "Schreibe die letzten $ANZAHL Commits neu ..."
git rebase -r HEAD~$ANZAHL --exec "git commit --amend --no-edit --reset-author"

echo
echo "=============================================="
echo " Ergebnis:"
echo "=============================================="
git log -$ANZAHL --format="  %h  %an <%ae>%n      %s"
echo

# --- Push ---
read -p "Jetzt zu GitHub pushen (force-with-lease)? [j/N] " push
if [ "$push" = "j" ] || [ "$push" = "J" ]; then
    git push --force-with-lease
    echo
    echo "Fertig. Schreib buchen kurz, dass die Commits neu gepusht sind."
else
    echo "Nicht gepusht. Wenn Du soweit bist:"
    echo "    git push --force-with-lease"
fi
