$swagger="swagger.yaml"

curl http://developer.augmented.city/spec/v2 -o "doc/$swagger"

docker run `
  --rm `
  -v $PWD/doc:/usr/local/src `
  -v $PWD/reloc-api:/usr/local/dist `
  openapitools/openapi-generator-cli `
    generate `
      -i /usr/local/src/$swagger `
      -g kotlin `
      -o /usr/local/dist `
      --package-name com.ac.api `
      --library jvm-retrofit2 `
      --artifact-id ac-api `
      --artifact-version 2.0.0 `
      --group-id com.ac.api
      #--source-folder src/main doesn't work for some reason
