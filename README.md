## Start the app

### 1. Start backend

#### - tab 1

```shell
sbt "~bookerService/reStart"
```

### 2. Start frontend

#### - tab 2

```shell
sbt "~bookerUI/fastLinkJS"
```

#### - tab 3

```shell
cd booker-ui
#only has to be run once
npm install
yarn exec vite
```

Then you can access the frontend at http://localhost:3000
and login with user/pass: john/aaa or jane/aaa

Once logged in you should see this screen:
![](https://github.com/adrianfilip/reservation-boker/blob/master/Screenshots/MyReservationsPage.png?raw=true)

## 2. Other commands

```shell
sbtn "~bookerService/reStart"

sbtn "~bookerUI/fastLinkJS"

cd booker-ui
yarn exec vite

curl -X POST http://localhost:8090/login -H 'Content-Type:application/json' -d '{"username":"my_login","password":"my_password"}' | jq

curl -v -X POST http://localhost:8090/login -H 'Content-Type:application/json' -d '{"username":"my_login","password":"my_password"}' | jq

#ports
lsof -PiTCP -sTCP:LISTEN
```

## 3. Other details

This project is a Proof of Concept for Scala 3 + ZIO 2 + zio-http + Laminar so not all simplifications or refactors that
can be done are done.