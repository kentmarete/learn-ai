import logging

import uvicorn

if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO)
    uvicorn.run(
        "agent_app.api:app",
        app_dir="src",
        host="0.0.0.0",
        port=8000,
        reload=True,
    )
