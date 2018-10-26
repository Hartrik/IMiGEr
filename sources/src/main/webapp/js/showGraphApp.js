/**
 * Application running on the ShowGraph page.
 */
class ShowGraphApp extends App {
	/**
	 * @inheritdoc
	 */
	constructor(appName, appHomeUrl) {
		super(appName, appHomeUrl);

		this.constants = new Constants;
		this.graphLoader = new GraphLoader;
		this.graphExporter = new GraphExporter;
		this.loader = new Loader;
		this.zoom = new Zoom(0.8);
		this.markSymbol = new MarkSymbol;

		/** @prop {array<Edge>} edgeList */
		this.edgeList = [];
		/** @prop {array<(Vertex|Group)>} nodeList */
		this.nodeList = [];
		/** @prop {array<Vertex>} vertexList */
		this.vertexList = [];
		/** @prop {array<Group>} groupList */
		this.groupList = [];

		/** @prop {Diagram} diagram */
		this.diagram = null;

		/** @prop {object} archetype Object containing all vertex and edge archetypes used in currently displayed diagram. */
		this.archetype = {
			vertex: [],
			edge: [],
		};

		/** @prop {array} attributeTypeList Array containing all possible types of vertex/edge attributes. */
		this.attributeTypeList = [];
		/** @prop {array} possibleEnumValues Array containing all possible values of attributes with datatype ENUM. */
		this.possibleEnumValues = {};
	}

	/**
	 * Initiates the application.
	 * @param {function} startFn Function to be run to load graph data.
	 */
	run(diagramId) {
		console.log('running...');

		this._bootstrap();
		this._loadGraphData(diagramId);
	}

	/**
	 * Resets the application to the state as it was when the graph was loaded for the first time.
	 */
	reset() {
		this.viewportComponent.reset();
		this.sidebarComponent.reset();

		this.edgeList = [];
		this.nodeList = [];
		this.vertexList = [];
		this.groupList = [];
	}

	/**
	 * Finds a vertex by its name.
	 * @param {string} name Name of the searched vertex.
	 */
	findVertexByName(name) {
		return this.vertexList.find(existingVertex => {
			return existingVertex.name == name;
		});
	}

	/**
	 * Closes components floating above viewport (context menu and popovers).
	 */
	closeFloatingComponents() {
		this.viewportComponent.contextMenuComponent.close();
		this.viewportComponent.vertexPopoverComponent.close();
		this.viewportComponent.edgePopoverComponent.close();
	}

	/**
	 * Redraws edges leading from viewport to sidebar.
	 */
	redrawEdges() {
		this.sidebarComponent.refreshFloaters();
	}

	/**
	 * Binds user interactions to local handler functions.
	 */
	_bootstrap() {
		this.headerComponent = new Header;
		this.navbarComponent = new Navbar;
		this.viewportComponent = new Viewport;
		this.sidebarComponent = new Sidebar;
		this.modalWindowComponent = new SaveDiagramModalWindow;

		const appElement = document.getElementById('app');
		appElement.appendChild(this.headerComponent.render());
		appElement.appendChild(this.navbarComponent.render());
		appElement.appendChild(DOM.h('main', {
			class: 'graph-content',
			id: 'content',
		}, [
			this.viewportComponent.render(),
			this.sidebarComponent.render(),
		]));
		appElement.appendChild(this.modalWindowComponent.render());

		this.sidebarComponent.minimapComponent.viewportSize = this.viewportComponent.getSize();

		// diagram
		document.addEventListener(DiagramUpdatedEvent.name, e => {
			this.diagram = new Diagram(e.detail);

			document.title = this.name + ' - ' + this.diagram.name;
			history.replaceState({} , document.title, this.homeUrl + 'graph?diagramId=' + this.diagram.id);
		});

		// context menu
		document.body.addEventListener('mousedown', () => {
			this.closeFloatingComponents();
		});

		// zoom
		document.getElementById('zoomValue').innerText = Math.round(this.zoom.scale * 100) + '%';
		document.getElementById('graph').setAttribute('transform', 'scale(' + this.zoom.scale + ')');

		// window resize
		window.addEventListener('resize', e => {
			this.redrawEdges();
			this.sidebarComponent.minimapComponent.viewportSize = this.viewportComponent.getSize();
		});
	}

	/**
	 * Loads graph data of a diagram.
	 * @param {string} diagramId Identifier of the diagram to be loaded.
	 */
	async _loadGraphData(diagramId) {
		this.loader.enable();

		let loadGraphDataPromise;

		if (diagramId === '') {
			loadGraphDataPromise = AJAX.getJSON(Constants.API.loadGraphData);

		} else {
			const diagramData = await AJAX.getJSON(Constants.API.getDiagram + '?id=' + diagramId);

			this.diagram = new Diagram(diagramData);

			document.title = this.name + ' - ' + this.diagram.name;

			loadGraphDataPromise = Promise.resolve(JSON.parse(diagramData.graph_json));
		}

		try {
			// get vertex position data
			const graphData = await loadGraphDataPromise;

			// construct graph
			this.graphLoader.run(graphData);

			this.loader.disable();

		} catch (error) {
			if (error instanceof HttpError) {
				switch (error.response.status) {
					case 401:
						alert('You are not allowed to view the diagram.');
						break;
					default:
						alert('Something went wrong.');
				}
			} else {
				alert('Something went wrong. Check console for more details.');
				console.error(error);
			}

			// go to the upload page
			window.location.replace('./');
		}
	}
}
