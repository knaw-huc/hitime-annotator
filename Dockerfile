FROM golang:1.12-alpine as buildserver

# Git is needed by go get.
RUN apk add --no-cache git

WORKDIR /go/src/github.com/knaw-huc/hitime-annotator
COPY . .

RUN go get -t -v ./...
RUN CGO_ENABLED=0 go test ./...
RUN CGO_ENABLED=0 go install -ldflags="-s" .


FROM node:11-alpine as buildui

WORKDIR /hitime

# Setup default REACT_APP_HOST for use on localhost
# To override, use "--build-arg DEPLOY_URL=<value>"
ARG DEPLOY_URL=http://localhost:8080
ENV REACT_APP_HOST=$DEPLOY_URL

COPY ui/package.json .
COPY ui/package-lock.json .
COPY ui/tsconfig.json .
COPY ui/public public
COPY ui/src src

RUN npm install
RUN npm run build


FROM scratch
COPY --from=buildserver /go/bin/hitime-annotator .
COPY --from=buildui /hitime/build ./ui

EXPOSE 8080

ENTRYPOINT ["./hitime-annotator", ":8080", "/data/todo.json.gz"]
