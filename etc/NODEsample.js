var express = require('express');
var http = require('http');
var bodyParser = require('body-parser');
var app = express();

app.set('port', process.env.PORT || 5000);
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

//첫 번째 미들웨어
app.use(function (req, res, next) {

    var position ={'latitude':'0','longitude':'0'};
    var paramlatitude = req.body.latitude;
    var paramlongitude = req.body.longitude;
    console.log('latitude : ' + paramlatitude + ' longitude : ' + paramlongitude);

    position.latitude = paramlatitude;
    position.longitude = paramlongitude;

    res.send(position);
});

var server = http.createServer(app).listen(app.get('port'), function () {
    console.log("익스프레스로 웹 서버를 실행 : " + app.get('port'));
});