# Student Pass/Fail Prediction: a small machine-learning demo

This project trains a **logistic regression classifier** to estimate whether a student is likely to pass or fail.

## What the model learns from

The sample CSV contains fictional student records with four input features:

- `study_hours_per_week`: hours spent studying each week
- `attendance_percent`: attendance from 0 to 100
- `previous_exam_score`: previous exam score from 0 to 100
- `assignments_submitted_out_of_4`: assignments submitted, from 0 to 4

The target column, `result`, contains `Pass` or `Fail`.

## 1. Set up

Python 3.10 or newer is recommended.

```bash
python -m venv .venv
```

Activate the environment:

**macOS/Linux**
```bash
source .venv/bin/activate
```

**Windows PowerShell**
```powershell
.venv\Scripts\Activate.ps1
```

Install dependencies:

```bash
pip install -r requirements.txt
```

## 2. Train the model

```bash
python train.py
```

The script:
1. Loads the CSV dataset.
2. Splits the records into training and test sets.
3. Scales the numeric features.
4. Trains logistic regression.
5. Prints accuracy, a classification report, and a confusion matrix.
6. Saves the trained model under `models/`.

## 3. Predict for a student

```bash
python predict.py
```

Open `predict.py` and change the values in the `student` dictionary to test different profiles. Run it again to see the predicted class and estimated pass probability.

## How to explain this to students

- **Features** are the information given to the model, such as attendance and study hours.
- **Label** is the answer the model learns to predict, here `Pass` or `Fail`.
- **Training** means showing the algorithm examples with known outcomes.
- **Testing** means checking predictions against examples the model did not train on.
- **Probability** is the model's estimated confidence for each class. It is not a guarantee.

Logistic regression is a supervised machine-learning classification algorithm. The pipeline standardizes the input values before fitting the classifier.

## Important limitations

This is a small, fictional teaching dataset. Its patterns are simplified and its results are not suitable for decisions about real students. A real model would need a larger, representative, ethically collected dataset, careful validation, privacy safeguards, and checks for bias. A model can also learn misleading patterns from poor-quality labels or data.
