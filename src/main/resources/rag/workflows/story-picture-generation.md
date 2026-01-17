# Story Picture Generation Workflow

Summary: A two-step workflow for brainstorming a story concept and generating a fitting picture. First, an optimized image prompt is engineered from the story concept, then the image is generated using DALL-E 3.

## Steps

1. **Prompt Engineering**: Use `ImagePromptEngineerOperator` to transform the story concept into an optimized image generation prompt
   - Input: The story concept or idea
   - Output: An optimized prompt and negative prompt for image generation

2. **Image Generation**: Use `ImageGenerationOperator` to generate the actual image
   - Input: The optimized prompt from step 1
   - Output: Generated image URL and artifact ID

## Inputs

- Story concept or idea (natural language description)
- Optional: Desired artistic style
- Optional: Mood or atmosphere

## Outputs

- Generated image (stored as artifact)
- Image URL for viewing
- Artifact ID for tracking

## Operators Used

- `ImagePromptEngineerOperator`: Crafts optimal prompts for AI image generation
- `ImageGenerationOperator`: Generates images using DALL-E 3

## Example

Task: "Create a picture for a story about a lonely robot finding friendship in a post-apocalyptic garden"

Step 1 (ImagePromptEngineerOperator):
- Input: concept = "a lonely robot finding friendship in a post-apocalyptic garden"
- Output: optimizedPrompt = "A weathered humanoid robot with gentle glowing eyes, kneeling beside a small flowering plant in an overgrown post-apocalyptic garden, soft golden hour lighting, cinematic composition, detailed mechanical parts with rust and moss, hopeful atmosphere"

Step 2 (ImageGenerationOperator):
- Input: prompt = [optimized prompt from step 1]
- Output: imageUrl, artifactId
