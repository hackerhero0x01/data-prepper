# Copyright OpenSearch Contributors
# SPDX-License-Identifier: Apache-2.0

ARG DOCKER_FILE_DIR
ARG FROM_IMAGE

FROM ${FROM_IMAGE}

ARG DATA_PREPPER_VERSION
ARG BUILD_NAME
ARG TAR_FILE

ENV DATA_PREPPER_HOME="/usr/share/data-prepper"

RUN mkdir ${DATA_PREPPER_HOME}
WORKDIR ${DATA_PREPPER_HOME}

COPY ${DOCKER_FILE_DIR}/${TAR_FILE} ${DATA_PREPPER_HOME}
RUN tar -xzf "${DATA_PREPPER_HOME}/${TAR_FILE}" -C "${DATA_PREPPER_HOME}" --strip-components=1 \
    && rm -f "${DATA_PREPPER_HOME}/${TAR_FILE}"

ENTRYPOINT "${DATA_PREPPER_HOME}/bin/data-prepper"
