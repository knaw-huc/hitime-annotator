import config from "../config";

class Resources {

    public static getStatistics = () => {
        return fetch(`${config.HOST}${config.API}/statistics`);
    };

    public static getRandomIndex = () => {
        return fetch(`${config.HOST}${config.API}/randomindex`);
    };

    public static getItem = (index: number) => {
        return fetch(`${config.HOST}${config.API}/item/${index}`);
    };

    public static putAnnotation = (index: number, annotation: string) => {
        return fetch(`${config.HOST}${config.API}/item/${index}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'text/plain',
            },
            body: annotation
        });
    };

}

export default Resources;
