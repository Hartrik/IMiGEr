class Layouts {

    static circularLayout(vertices, center) {
        let MIN_DISTANCE = 200;
        let MAX_DISTANCE = 400;

        // TODO: better computations
        let radius = MIN_DISTANCE + (vertices.length / 100 * (MAX_DISTANCE - MIN_DISTANCE));

        vertices.forEach((v, i) => {
            let angle = (i / vertices.length) * (Math.PI * 2);
            let coordinates = new Coordinates(
                center.x + radius * Math.cos(angle) - (v.size.width / 2),
                center.y + radius * Math.sin(angle)
            );
            v.position = coordinates;
            v.move(coordinates);
        });
    }

}
