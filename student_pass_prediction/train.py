"""Train a simple model to predict whether a student is likely to pass."""
from pathlib import Path
import joblib
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix

ROOT = Path(__file__).resolve().parent
DATA = ROOT / "data" / "student_performance.csv"
MODEL_DIR = ROOT / "models"
MODEL_DIR.mkdir(exist_ok=True)

df = pd.read_csv(DATA)
features = [
    "study_hours_per_week",
    "attendance_percent",
    "previous_exam_score",
    "assignments_submitted_out_of_4",
]
X = df[features]
y = df["result"]

X_train, X_test, y_train, y_test = train_test_split(
    X, y, test_size=0.25, random_state=42, stratify=y
)

# A pipeline scales the inputs, then trains logistic regression.
model = Pipeline([
    ("scaler", StandardScaler()),
    ("classifier", LogisticRegression(random_state=42)),
])
model.fit(X_train, y_train)

predictions = model.predict(X_test)
print(f"Test accuracy: {accuracy_score(y_test, predictions):.2%}")
print("\nClassification report:")
print(classification_report(y_test, predictions, zero_division=0))
print("Confusion matrix (rows=actual, columns=predicted; order Fail, Pass):")
print(confusion_matrix(y_test, predictions, labels=["Fail", "Pass"]))

model_path = MODEL_DIR / "pass_prediction_model.joblib"
joblib.dump(model, model_path)
print(f"\nSaved trained model to: {model_path}")
