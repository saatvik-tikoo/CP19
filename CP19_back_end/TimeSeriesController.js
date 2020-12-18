module.exports = function (app, rdb, jsonParser, urlencodedParser, cors, MongoClient) {

    app.use(jsonParser);
    app.use(urlencodedParser);

    var url = "mongodb+srv://dbUser:16RFespRjWMqmVuQ@cluster0.f5cdp.mongodb.net/test?authSource=admin&replicaSet=atlas-12rpma-shard-0&readPreference=primary&appname=MongoDB%20Compass&ssl=true";

    // Run every 4 hours
    setInterval(function(){
        MongoClient.connect(url, { useNewUrlParser: true, useUnifiedTopology: true }, function (err, db) {
            if (err) throw err;
    
            var dbo = db.db("Covid");
            dbo.collection("Locations").find({}).toArray(function(err, result) {
                if (err) throw err;
                
                // Get all the locations together and check the color and upadte the markers table and the user table
                db.close();
            });
        });
    }, 14400)
}