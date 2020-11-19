FROM mozilla/sbt:8u232_1.3.8@sha256:ce57ee29b80991d68c00d698d96cf123e693aa871afa10d12f97fb147177552c

RUN mkdir -p /srv/app

COPY ./ /srv/app

WORKDIR /srv/app

EXPOSE 9000

CMD ["sbt", "clean", "run"]
