# Chat Kafka

[![Run on Google Cloud](https://deploy.cloud.run/button.png)](https://deploy.cloud.run)

Run the server:
```
./gradlew bootRun
```

Open:
[localhost:8080](http://localhost:8080)

Create container:
```
./gradlew bootBuildImage --imageName=chat-kafka
```

```
{"specversion":"1.0","id":"b4d4ec89-a287-4319-aef8-7c024c42c0d9","source":"http://explicit-filter.default.svc.cluster.local/filter","type":"text-filtered","datacontenttype":"text/plain;charset=UTF-8","subject":"partition:0#47","time":"2021-10-28T17:15:57.107Z","knativearrivaltime":"2021-10-28T17:16:07.749099075Z","key":"2ab6853b","data_base64":"eyJib2R5IjoiYXNkZiBmb28ifQ=="}
```