module.exports = function (app, db, jsonParser, urlencodedParser, cors) {

    app.use(jsonParser);
    app.use(urlencodedParser);

    console.log("Registering patch endpoint: /api/user/register/");

    app.patch("/api/user/register/", cors(), (req, res) => {
        var sql = 'UPDATE users SET email = ' + req.query.email + ' WHERE id = ' + req.query.id + ";";
        db.run(sql, function (err, result) {
            if (err) {
                res.status(400).json({ "error": err.message })
                return;
            }
            res.json({
                "message": "success",
                "data": result
            })
        });
    })

    console.log("Registering patch endpoint: /api/user/covidmarker/");

    app.patch("/api/user/covidmarker/", cors(), (req, res) => {
        var sql = 'UPDATE users SET covid = ' + req.query.covid + ' WHERE id = ' + req.query.id + ";";
        db.run(sql, function (err, result) {
            if (err) {
                res.status(400).json({ "error": res.message })
                return;
            }
            res.json({
                message: "success",
                data: result
            })
        });
    })

}