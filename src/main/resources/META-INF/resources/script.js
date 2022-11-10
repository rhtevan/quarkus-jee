class Source {

	constructor() {
		this.source = new EventSource("http://localhost:8080/hello");
		this.source.onopen    = (e) => console.log(e);
		this.source.onmessage = ({data})  => this.updateDoc(data);
	}

	updateDoc(data) {
		document.getElementById("result").innerHTML += data + "<br>";
	}
}

new Source();