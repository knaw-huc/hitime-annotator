# Build server (backend) image.
FROM golang:1.12-alpine as buildserver

# Git is needed by go mod download.
RUN apk add --no-cache git

WORKDIR /build

# Download dependencies in a separate step from the actual build,
# so they remain cached when the sources change
# (https://container-solutions.com/faster-builds-in-docker-with-go-1-11/).
COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 go test ./...
RUN CGO_ENABLED=0 go build -ldflags="-s" .


# Build UI (frontend) image.
FROM node:11-alpine as buildui

WORKDIR /hitime

# Setup default REACT_APP_HOST for use on localhost
# To override, use "--build-arg DEPLOY_URL=<value>"
ARG DEPLOY_URL=https://hitimep.tt.di.huc.knaw.nl
ENV REACT_APP_HOST=$DEPLOY_URL

COPY ui/package.json .
COPY ui/package-lock.json .
COPY ui/tsconfig.json .
COPY ui/public public
COPY ui/src src

RUN npm install
RUN npm run build


# Combine server and UI into a deployable image.
FROM scratch
COPY --from=buildserver /build/hitime-annotator .
COPY --from=buildui /hitime/build ./ui

EXPOSE 8080

ENTRYPOINT ["./hitime-annotator", ":8080", "/data/todo.json.gz"]
