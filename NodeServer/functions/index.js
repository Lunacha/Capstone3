const functions = require('firebase-functions');
const express = require('express');
const app = express();
var http = require('http');
var bodyParser = require('body-parser');
var test = require('./test.js');

app.set('port', process.env.PORT || 5000);
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

app.use('/', (req, res) => {

    var lostposition = { "latitude": 0, "longitude": 0 };
    var losttime = req.body.losttime;
    var targetspeed = req.body.targetspeed;
    var lostlatitude = req.body.latitude;
    var lostlongitude = req.body.longitude;
    lostposition.latitude = lostlatitude;
    lostposition.longitude = lostlongitude;
    
    //디버깅용
    // var losttime = 337.8; // 실종 경과 시간
    // var targetspeed = 1.04; //아이의 평균 보행속도(m/s)
    // var lostposition = { "latitude": 37.295975917528786, "longitude": 126.9726127758622 };
    var result = test.mainfucntions(losttime, targetspeed, lostposition);

    res.send(result);
});

app.use('/api', (req, res) => {
    
    var result = test.mainfucntions();

    res.send(result);
});

//로컬 테스트용 - 주석 해제후 firebase부분 + 마지막줄 주석처리
//http.createServer(app).listen(app.get('port'), function () {
//  console.log("익스프레스로 웹 서버를 실행 : " + app.get('port'));
//}); 

exports.app = functions.https.onRequest(app);  