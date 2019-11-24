const functions = require('firebase-functions');
const express = require('express');
const app = express();
var http = require('http');
var bodyParser = require('body-parser');

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
    var position ={'latitude':'0','longitude':'0'};

    position.latitude = "12345";
    position.longitude = "11123";

    res.send(position);
});

exports.app = functions.https.onRequest(app);
  

  