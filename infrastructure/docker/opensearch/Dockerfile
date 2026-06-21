FROM opensearchproject/opensearch:latest

COPY --chmod=755 ../../../config/opensearch/opensearch-docker-init.sh /usr/local/bin/init-opensearch.sh
COPY --chmod=644 ../../../config/opensearch/opensearch-template.json /usr/local/bin/opensearch-template.json

# Override entrypoint to run custom init script
ENTRYPOINT ["/usr/local/bin/init-opensearch.sh"]