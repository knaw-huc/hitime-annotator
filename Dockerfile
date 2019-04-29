FROM golang:1.12-alpine as buildserver

# Git is needed by go get.
RUN apk add --no-cache git

WORKDIR /go/src/github.com/knaw-huc/hitime-annotator
COPY . .

RUN go get -t -v ./...
RUN CGO_ENABLED=0 go test ./...
RUN CGO_ENABLED=0 go install -ldflags="-s" .


FROM scratch
COPY --from=buildserver /go/bin/hitime-annotator .

EXPOSE 8080

ENTRYPOINT ["./hitime-annotator", ":8080", "/data/todo.json.gz"]
