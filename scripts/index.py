import argparse
import os
import sys

from groq import Groq


GROQ_API_KEY = os.environ.get("GROQ_API_KEY")
SUMMARY_MODEL = "openai/gpt-oss-120b"
SUMMARY_PROMPT = """You are given a raw speech-to-text transcript, which may
contain filler words, repetition, or minor recognition errors typical of
automatic transcription.

Read it and produce a clean, well-formatted summary in Markdown with this
exact structure:

# Summary

A short 2-3 sentence overview of what was said.

# Key Points

* A bullet list of the main points, one per line.
* Keep each bullet concise (one sentence).
* Preserve the original meaning; do not invent information not present
  in the transcript.

# Notable Details

* Any specific names, numbers, dates, or action items mentioned (omit
  this section if none exist).

# category

Classify the customer support conversation into exactly ONE of the following
five categories. Return only the category name.

1. TECHNICAL

   * Bugs, errors, system problems, website/app problems, technical issues,
     integration problems, or performance issues.

2. HELP

   * How-to questions, product information, general inquiries, guidance,
     usage questions, or requests for assistance.

3. ACCOUNT_BILLING

   * Login/account access, password issues, payments, billing, invoices,
     subscriptions, or account cancellation.

4. ORDER_SERVICE

   * Order problems, delivery/shipping, returns, refunds, cancellations,
     or problems with the provided service.

5. FEEDBACK_REQUEST

   * Feature requests, product suggestions, general feedback, complaints,
     or requests for improvements.

Choose the category based on the main reason for the customer's contact.
Do not create a new category or return multiple categories.

Return ONLY the formatted Markdown above - no preamble, no explanation,
no extra commentary before or after it."""


def transcribe(file_path, api_key, model, language=None):
    client = Groq(api_key=api_key)

    with open(file_path, "rb") as audio_file:
        kwargs = {
            "file": audio_file,
            "model": model,
            "response_format": "verbose_json",
        }
        if language:
            kwargs["language"] = language
        result = client.audio.transcriptions.create(**kwargs)
    return result


def summarize_key_points(transcript_text, api_key):
    client = Groq(api_key=api_key)
    response = client.chat.completions.create(
        model=SUMMARY_MODEL,
        messages=[
            {"role": "system", "content": SUMMARY_PROMPT},
            {"role": "user", "content": transcript_text},
        ],
        temperature=0.2,
    )
    return response.choices[0].message.content.strip()


def main():
    parser = argparse.ArgumentParser(
        description="Transcribe an audio file with Groq's Whisper API")
    parser.add_argument(
        "audio_path", help="Path to the audio file (mp3, wav, m4a, etc.)")
    parser.add_argument("--model", default="whisper-large-v3",
                        help="whisper-large-v3 (default, most accurate - recommended for "
                        "Sinhala and other lower-resource languages) or "
                        "whisper-large-v3-turbo (faster, best for English)")
    parser.add_argument("--api-key", default=None,
                        help="Groq API key (or set GROQ_API_KEY environment variable)")
    parser.add_argument("--out", default=None,
                        help="Output .md file for the formatted key points "
                        "(default: <audio_filename>_keypoints.md)")
    parser.add_argument("--language", default=None,
                        help="ISO 639-1 language code to force (e.g. 'si' for Sinhala). "
                        "Recommended for lower-resource languages to avoid "
                        "auto-detection mistakes; omit to auto-detect.")
    args = parser.parse_args()

    api_key = args.api_key or GROQ_API_KEY
    if not api_key:
        sys.exit("No API key set. Set GROQ_API_KEY or pass --api-key.")

    if not os.path.exists(args.audio_path):
        sys.exit(f"File not found: {args.audio_path}")

    print(f"Uploading '{args.audio_path}' to Groq ({args.model})...")
    result = transcribe(args.audio_path, api_key, args.model, args.language)

    transcript_text = result.text
    print(f"Sending transcript to {SUMMARY_MODEL} for key-point formatting...")
    formatted = summarize_key_points(transcript_text, api_key)

    out_path = args.out or os.path.splitext(
        args.audio_path)[0] + "_keypoints.md"
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(formatted)
    print(f"\nSaved formatted key points to {out_path}")


if __name__ == "__main__":
    main()
