"""Predict pass/fail likelihood for one example student."""
from pathlib import Path
import joblib
import pandas as pd

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "models" / "pass_prediction_model.joblib"

if not MODEL_PATH.exists():
    raise SystemExit("Model not found. Run: python train.py")

model = joblib.load(MODEL_PATH)

# Change these example values to experiment with different students.
student = {
    "study_hours_per_week": 7,
    "attendance_percent": 82,
    "previous_exam_score": 68,
    "assignments_submitted_out_of_4": 3,
}
X_new = pd.DataFrame([student])
predicted_class = model.predict(X_new)[0]
probabilities = model.predict_proba(X_new)[0]
classes = model.named_steps["classifier"].classes_
pass_probability = probabilities[list(classes).index("Pass")]

print("Student profile:")
for key, value in student.items():
    print(f"  {key.replace('_', ' ')}: {value}")
print(f"\nPrediction: {predicted_class}")
print(f"Estimated probability of passing: {pass_probability:.1%}")
print("\nThis is a classroom demonstration, not a reliable assessment of a real student.")
