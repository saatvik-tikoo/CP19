module.exports = function (app, db, jsonParser, urlencodedParser, cors, request) {

	app.use(jsonParser);
	app.use(urlencodedParser);

	console.log("Registering get endpoint: /api/markers");

	app.get('/api/markers', cors(), function (req, res) {
		// 1. Get city name from the lat and long
		let locationUrl = "https://geocode.xyz/" + req.query.lat + "," + req.query.long + "?geoit=json";

		request(locationUrl)
			.then((json) => {
				let data = JSON.parse(json);
				let city = data.city;
				let state = data.statename;

				// 2. Use data API to get the city color 
				let markerUrl = "https://covid-19-statistics.p.rapidapi.com/reports?region_province=" + state +
					"&iso=USA&region_name=US&city_name=" + city + "&date=2020-04-16&q=US " + state;

				request({
					uri: markerUrl,
					headers: {
						"x-rapidapi-host": "covid-19-statistics.p.rapidapi.com",
						"x-rapidapi-key": ""
					}
				})
					.then((json) => {
						let markerData = JSON.parse(json).data[0];
						let cases = markerData.region.cities[0].confirmed;
						let deaths = markerData.region.cities[0].deaths;
						let apiBasedColor;

						if (cases < 1000 && deaths < 50) apiBasedColor = "Green"
						else if (cases >= 1000 && cases < 5000 && deaths < 50) apiBasedColor = "Yellow"
						else apiBasedColor = "Red"

						// 3. Use (+/-) 0.1 to get all locations to show
						let sql = "SELECT * FROM markers WHERE Lat BETWEEN " + (parseFloat(req.query.lat) - 0.1) + " AND " +
							(parseFloat(req.query.lat) + 0.1) + " AND Long BETWEEN " + (parseFloat(req.query.long) - 0.1) + " AND " +
							(parseFloat(req.query.long) + 0.1);

						db.all(sql, function (err, locationData) {
							if (err) {
								res.status(400).json({ "error": err.message });
								return;
							}
							if (locationData.length == 0) {
								res.json({
									"message": "success",
									"data": [
										{
											"Lat": req.query.lat,
											"Long": req.query.long,
											"Color": apiBasedColor
										}
									]
								})
							}
							else {
								// 4. Choose appropriate color to show for all the above locations.
								let notFound = true;
								for (loc = 0; loc < locationData.length; loc++) {
									if (apiBasedColor == "Red") {
										if (locationData[loc].Color == "R") {
											locationData[loc].Color = "Red"
										} else if (locationData[loc].Color == "Y") {
											locationData[loc].Color = "Yellow"
										} else if (locationData[loc].Color == "G") {
											locationData[loc].Color = "Green"
										}
									} else if (apiBasedColor == "Yellow") {
										if (locationData[loc].Color == "R") {
											locationData[loc].Color = "Red"
										} else if (locationData[loc].Color == "Y") {
											locationData[loc].Color = "Yellow"
										} else if (locationData[loc].Color == "G") {
											locationData[loc].Color = "Green"
										}
									} else if (apiBasedColor == "Green") {
										if (locationData[loc].Color == "R") {
											locationData[loc].Color = "Red"
										} else if (locationData[loc].Color == "Y") {
											locationData[loc].Color = "Yellow"
										} else if (locationData[loc].Color == "G") {
											locationData[loc].Color = "Green"
										}
									}

									if (locationData[loc].Lat == req.query.lat && locationData[loc].Long == req.query.lat)
										notFound = false;
								}
								if (notFound) {
									locationData.push({
										"Lat": req.query.lat,
										"Long": req.query.long,
										"Color": apiBasedColor
									});
								}
								res.json({
									"message": "success",
									"data": locationData
								})
							}
						});

					})
					.catch((error) => {
						console.log(error);
						res.status(400).json({ "message": error });
					});
			})
			.catch((error) => {
				console.log(error);
				res.status(400).json({ "message": error });
			});
	});

}