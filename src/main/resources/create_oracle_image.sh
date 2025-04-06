docker run --name test \
>          -p 1521:1521 \
>          -e ORACLE_RANDOM_PASSWORD="y" \
>          -v oracle_init_scripts:/container-entrypoint-initdb.d


