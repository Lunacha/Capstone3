const functions = require('firebase-functions');
const express = require('express');
const app = express();
var http = require('http');
var bodyParser = require('body-parser');
var test = require('./test.js');

app.set('port', process.env.PORT || 5000);
app.use(bodyParser.urlencoded({ extended: false }));
app.use(bodyParser.json());

app.get('/', (req, res) => {

    var position ={'latitude':'0','longitude':'0'};

    position.latitude = "12345";
    position.longitude = "11123";

    res.send(position);
});

app.get('/api', (req, res) => {
    
    var result = test.mainfucntions();

    res.send(result);
});

//로컬 테스트용 - 주석 해제후 firebase부분 + 마지막줄 주석처리
// http.createServer(app).listen(app.get('port'), function () {
//     console.log("익스프레스로 웹 서버를 실행 : " + app.get('port'));
// }); 

exports.app = functions.https.onRequest(app);
  

  