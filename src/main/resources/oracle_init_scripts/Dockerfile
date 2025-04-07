FROM gvenzl/oracle-free:23.6-slim-faststart

# Copy SQL scripts
COPY sql/*.sql /docker-entrypoint-initdb.d/

# Set environment variables
ENV ORACLE_PASSWORD=oracle
ENV ORACLE_DATABASE=orcl

# Expose Oracle port
EXPOSE 1521 