set -e

CONTAINER=$(docker ps -f "name=test-db" -q)
if [ -z "$CONTAINER" ]; then
  CONTAINER=$(docker run --name test-db --rm -d -p 5432:5432 postgres:12)
  sleep 5
fi
echo "$CONTAINER"

NAME="flashcards"

docker exec -i "$CONTAINER" psql -U postgres << EOF
DROP DATABASE IF EXISTS ${NAME};
DROP ROLE IF EXISTS ${NAME};

CREATE USER ${NAME} WITH PASSWORD '${NAME}';
CREATE DATABASE ${NAME};
GRANT ALL PRIVILEGES ON DATABASE ${NAME} TO ${NAME};
EOF

./gradlew :dbmigrate:run
