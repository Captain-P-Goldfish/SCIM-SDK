# Start the docker-compose setup

You can start the docker compose setup after the docker-images have been added to your local docker-registry with:

`docker-compose -p keycloak-scim-sdk up`

Please note that the docker-compose file needs to mount the script `wait-for-it.sh`. If you are using windows make
sure you have set this directory "./keycloak-docker" in your FileSharing settings in the docker-for-windows
application.