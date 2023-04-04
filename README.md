# Online Tracker

Tool to keep a history of when players were online

# Deployment steps

Build docker image
1. `cd online-tracker`
1. `sbt docker:publishLocal`

Copy to server
1. `docker images`
1. `docker save <image_id> | bzip2 | ssh server-host docker load`

On the server
1. Create a docker network: `docker network create online-tracker`
1. Start the postgres database container, substituting password and volume mount point if you need to:
`docker run --rm -d --network online-tracker -p 5432:5432 -e POSTGRES_PASSWORD=<password_here> -e PGDATA=/var/lib/postgresql/data/pgdata -v /home/tom/data/pgdata:/var/lib/postgresql/data --name online-tracker-postgres postgres'
1. Load the schema: `PGPASSWORD=<password_here> psql -h 0.0.0.0 -U postgres -f V01_init-schema.sql`
1. Create an env file with the database config (make sure DB_HOST matches the name of the postgres container, e.g. `online-tracker-postgres`).
1. Run the docker container for the app, pointing to the env file: `docker run --rm -d --env-file online-tracker.env --name online-tracker <image_id>`

You can now view the logs and connect to the database:
`docker logs -f --since=1h online-tracker`
`PGPASSWORD=<password_here> psql -h 0.0.0.0 -U postgres -d online_tracker`
