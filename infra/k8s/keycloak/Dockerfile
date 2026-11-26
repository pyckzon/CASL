# Imagen de Keycloak pre-augmentada para producción (ADR-021 §4).
# Permite arrancar con `start --optimized` y sistema de archivos de solo lectura.
FROM quay.io/keycloak/keycloak:26.8

ENV KC_HEALTH_ENABLED=true
RUN /opt/keycloak/bin/kc.sh build --db=postgres

# El realm se importa en runtime vía --import-realm (ConfigMap montado en /opt/keycloak/data/import).
USER 1000
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]