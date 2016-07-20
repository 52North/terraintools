// The Browser API key obtained from the Google Developers Console.
var developerKey = 'AIzaSyB2yc3lA0UTXlpB7EL9HVddU4YPo17ZwhU';

// The Client ID obtained from the Google Developers Console. Replace with your own Client ID.
var clientId = "948086675144-fq6u0ajvdi4mp8bt2ovh4p87mmrpesg7.apps.googleusercontent.com";

// Scope to use to access user's documents.
var scope = ['https://www.googleapis.com/auth/drive'];

var pickerApiLoaded = false;
var oauthToken;

var picker = null;

// Use the API Loader script to load google.picker and gapi.auth.
function onApiLoad() {
	gapi.load('auth', {'callback': onAuthApiLoad});
	gapi.load('picker', {'callback': onPickerApiLoad});
}

function onAuthApiLoad() {
	window.gapi.auth.authorize(
			{
				'client_id': clientId,
				'scope': scope,
				'immediate': true
			},
	handleAuthResult);
}

function onPickerApiLoad() {
	pickerApiLoaded = true;
	createPicker();
}

function handleAuthResult(authResult) {
	console.log(authResult);
	if (authResult && !authResult.error) {
		oauthToken = authResult.access_token;
		createPicker();
	}
}

// Create and render a Picker object for picking user Documents.
function createPicker() {
	if (pickerApiLoaded && oauthToken) {
		var folderView = new google.picker.View(google.picker.ViewId.FOLDERS);
		folderView.setQuery("52n-terraintools");
		picker = new google.picker.PickerBuilder().
				addView(folderView).
				setOAuthToken(oauthToken).
				setDeveloperKey(developerKey).
				setCallback(pickerCallback).
				build();
		picker.setVisible(false);
	}
}

// A simple callback implementation.
function pickerCallback(data) {
	var doc = null;
	if (data[google.picker.Response.ACTION] === google.picker.Action.PICKED) {
		doc = data[google.picker.Response.DOCUMENTS][0];
	}
	processDocument(doc);
}

// Should be overridden where picker.js is used
function processDocument(document){
	console.log(document);
}

// Make the picker visible
function showPicker() {
	picker.setVisible(true);
}