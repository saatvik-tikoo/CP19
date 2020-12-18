var express = require('express');
var bodyParser = require('body-parser');
var sqlite3 = require('sqlite3');
var cors = require('cors')
const request = require('request-promise')
var MongoClient = require('mongodb').MongoClient;

var app = express();
app.use(cors());
var jsonParser = bodyParser.json()
var urlencodedParser = bodyParser.urlencoded({ extended: false })

var db = new sqlite3.Database('./database/cp19db.db', (err) => { 
	if (err) { 
		console.log('Error when creating the database', err);
		throw 0;
	}
	console.log('Database accessed!');
});
 
// Add restful controller for User Data
require('./UserController')(app, db, jsonParser, urlencodedParser, cors);

// Add restful controller for Marker Data
require('./MarkersController')(app, db, jsonParser, urlencodedParser, cors, request);

// Add restful controller for Time Series Data
require('./TimeSeriesController')(app, db, jsonParser, urlencodedParser, cors, MongoClient);
 
// Serve static files
 
app.listen(8000);