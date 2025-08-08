import OpenAI from 'openai';

/**
 * OpenAI Service for CinematicBrand Director
 * Handles AI-powered brand research, story generation, and visual asset creation
 */

// Initialize OpenAI client
const openai = new OpenAI({
  apiKey: import.meta.env.VITE_OPENAI_API_KEY,
  dangerouslyAllowBrowser: true, // Required for client-side usage
});

/**
 * Analyzes a company by researching its brand identity and online presence
 * @param {string} brandName - Name of the brand to research
 * @returns {Promise<object>} Company analysis results
 */
export async function analyzeCompany(brandName) {
  try {
    const response = await openai?.chat?.completions?.create({
      model: 'gpt-4o',
      messages: [
        {
          role: 'system',
          content: `You are an expert brand analyst specializing in creating profiles for marketing video production. 
          Analyze the given brand and provide comprehensive brand intelligence that would be useful for creating cinematic marketing videos.
          Focus on brand personality, values, target audience, visual style, and competitive positioning.`
        },
        {
          role: 'user',
          content: `Analyze the brand "${brandName}" and provide a comprehensive brand profile. Include company background, brand values, target audience, visual identity, and key messaging that would be relevant for creating a cinematic marketing video.`
        }
      ],
      response_format: {
        type: 'json_schema',
        json_schema: {
          name: 'company_analysis',
          schema: {
            type: 'object',
            properties: {
              summary: {type: 'string'},
              keyPoints: {
                type: 'array',
                items: {type: 'string'}
              },
              brandPersonality: {type: 'string'},
              targetAudience: {type: 'string'},
              visualStyle: {type: 'string'},
              keyMessages: {
                type: 'array',
                items: {type: 'string'}
              },
              competitiveAdvantages: {
                type: 'array',
                items: {type: 'string'}
              }
            },
            required: ['summary', 'keyPoints', 'brandPersonality', 'targetAudience', 'visualStyle'],
            additionalProperties: false,
          },
        },
      },
      temperature: 0.7,
    });

    return JSON.parse(response?.choices?.[0]?.message?.content);
  } catch (error) {
    console.error('Error analyzing company:', error);
    throw new Error(`Failed to analyze company: ${error?.message || 'Unknown error'}`);
  }
}

/**
 * Generates a cinematic story outline based on brand analysis
 * @param {string} brandName - Name of the brand
 * @param {object} companyData - Company analysis data
 * @returns {Promise<object>} Story outline results
 */
export async function generateStoryOutline(brandName, companyData) {
  try {
    const response = await openai?.chat?.completions?.create({
      model: 'gpt-4o',
      messages: [
        {
          role: 'system',
          content: `You are a professional screenwriter specializing in cinematic marketing videos and commercial storytelling. 
          Create compelling narrative outlines that transform brands into cinematic stories suitable for high-end commercial production.
          Focus on emotional storytelling, visual metaphors, and brand transformation narratives.`
        },
        {
          role: 'user',
          content: `Based on this brand analysis for "${brandName}":
          
          Brand Summary: ${companyData?.summary}
          Brand Personality: ${companyData?.brandPersonality}
          Target Audience: ${companyData?.targetAudience}
          Visual Style: ${companyData?.visualStyle}
          Key Messages: ${companyData?.keyMessages?.join(', ') || 'N/A'}
          
          Create a cinematic story outline for a 60-90 second marketing video. The story should be formatted like a professional film treatment with scene descriptions, character development, and visual direction.`
        }
      ],
      response_format: {
        type: 'json_schema',
        json_schema: {
          name: 'story_outline',
          schema: {
            type: 'object',
            properties: {
              title: {type: 'string'},
              concept: {type: 'string'},
              storyOutline: {type: 'string'},
              keyScenes: {
                type: 'array',
                items: {
                  type: 'object',
                  properties: {
                    sceneNumber: {type: 'number'},
                    location: {type: 'string'},
                    description: {type: 'string'},
                    visualNotes: {type: 'string'}
                  },
                  required: ['sceneNumber', 'location', 'description']
                }
              },
              tone: {type: 'string'},
              duration: {type: 'string'}
            },
            required: ['title', 'concept', 'storyOutline', 'keyScenes', 'tone', 'duration'],
            additionalProperties: false,
          },
        },
      },
      temperature: 0.8,
    });

    return JSON.parse(response?.choices?.[0]?.message?.content);
  } catch (error) {
    console.error('Error generating story outline:', error);
    throw new Error(`Failed to generate story outline: ${error?.message || 'Unknown error'}`);
  }
}

/**
 * Generates visual concepts and character descriptions
 * @param {string} brandName - Name of the brand
 * @param {object} storyData - Story outline data
 * @param {object} companyData - Company analysis data
 * @returns {Promise<object>} Visual concepts results
 */
export async function generateVisualConcepts(brandName, storyData, companyData) {
  try {
    const response = await openai?.chat?.completions?.create({
      model: 'gpt-4o',
      messages: [
        {
          role: 'system',
          content: `You are a professional creative director specializing in visual concept development for commercial video production.
          Create detailed visual concepts including character descriptions, costume design, set design, and visual style guides.
          Focus on practical production considerations and detailed visual specifications.`
        },
        {
          role: 'user',
          content: `Based on this story for "${brandName}":
          
          Story Title: ${storyData?.title}
          Story Concept: ${storyData?.concept}
          Visual Style: ${companyData?.visualStyle}
          Key Scenes: ${storyData?.keyScenes?.map(
              scene => `Scene ${scene?.sceneNumber}: ${scene?.location} - ${scene?.description}`)
          ?.join('\n') || 'N/A'}
          
          Create detailed visual concepts including character designs, costume specifications, set designs, color palette, lighting style, and visual effects notes.`
        }
      ],
      response_format: {
        type: 'json_schema',
        json_schema: {
          name: 'visual_concepts',
          schema: {
            type: 'object',
            properties: {
              characters: {
                type: 'array',
                items: {
                  type: 'object',
                  properties: {
                    name: {type: 'string'},
                    description: {type: 'string'},
                    costume: {type: 'string'},
                    visualNotes: {type: 'string'}
                  },
                  required: ['name', 'description', 'costume']
                }
              },
              colorPalette: {
                type: 'array',
                items: {type: 'string'}
              },
              lightingStyle: {type: 'string'},
              setDesign: {
                type: 'array',
                items: {
                  type: 'object',
                  properties: {
                    location: {type: 'string'},
                    description: {type: 'string'},
                    props: {type: 'array', items: {type: 'string'}}
                  },
                  required: ['location', 'description']
                }
              },
              visualEffects: {type: 'string'},
              productPlacement: {type: 'string'}
            },
            required: ['characters', 'colorPalette', 'lightingStyle', 'setDesign'],
            additionalProperties: false,
          },
        },
      },
      temperature: 0.7,
    });

    return JSON.parse(response?.choices?.[0]?.message?.content);
  } catch (error) {
    console.error('Error generating visual concepts:', error);
    throw new Error(`Failed to generate visual concepts: ${error?.message || 'Unknown error'}`);
  }
}

/**
 * Generates visual assets using DALL-E
 * @param {string} brandName - Name of the brand
 * @param {object} visualConcepts - Visual concepts data
 * @param {object} storyData - Story data
 * @returns {Promise<array>} Array of generated image assets
 */
export async function generateVisualAssets(brandName, visualConcepts, storyData) {
  try {
    const imagePrompts = [];

    // Generate prompts based on key scenes and characters
    if (visualConcepts?.characters?.length > 0) {
      const mainCharacter = visualConcepts?.characters?.[0];
      imagePrompts?.push({
        prompt: `Professional commercial photography of ${mainCharacter?.description}, ${mainCharacter?.costume}, ${visualConcepts?.lightingStyle}, high-end commercial aesthetic, cinematic lighting, 4K resolution`,
        title: `Character: ${mainCharacter?.name}`,
        type: 'character'
      });
    }

    if (visualConcepts?.setDesign?.length > 0) {
      const mainSet = visualConcepts?.setDesign?.[0];
      imagePrompts?.push({
        prompt: `${mainSet?.description}, ${visualConcepts?.lightingStyle}, commercial photography style, professional lighting setup, ${visualConcepts?.colorPalette?.join(
            ' and ')} color scheme, cinematic composition`,
        title: `Set: ${mainSet?.location}`,
        type: 'set'
      });
    }

    // Generate product-focused image
    imagePrompts?.push({
      prompt: `Professional product photography for ${brandName}, clean modern aesthetic, commercial lighting, high-end brand presentation, ${visualConcepts?.colorPalette?.join(
          ' and ')} color palette, minimalist composition`,
      title: `Product: ${brandName}`,
      type: 'product'
    });

    // Generate scene stills
    if (storyData?.keyScenes?.length > 0) {
      const keyScene = storyData?.keyScenes?.[0];
      imagePrompts?.push({
        prompt: `Cinematic still from commercial video: ${keyScene?.description}, ${keyScene?.visualNotes
        || visualConcepts?.lightingStyle}, professional commercial cinematography, high production value`,
        title: `Scene: ${keyScene?.location}`,
        type: 'scene'
      });
    }

    // Generate images
    const assets = [];
    for (const imagePrompt of imagePrompts) {
      try {
        const imageResponse = await openai?.images?.generate({
          model: 'dall-e-3',
          prompt: imagePrompt?.prompt,
          n: 1,
          size: '1024x1024',
          quality: 'hd',
        });

        assets?.push({
          type: 'image',
          url: imageResponse?.data?.[0]?.url,
          title: imagePrompt?.title,
          description: imagePrompt?.prompt,
          category: imagePrompt?.type
        });

        // Add delay to avoid rate limits
        await new Promise(resolve => setTimeout(resolve, 1000));
      } catch (imageError) {
        console.error(`Error generating image for ${imagePrompt?.title}:`, imageError);
        // Continue with other images even if one fails
      }
    }

    return assets;
  } catch (error) {
    console.error('Error generating visual assets:', error);
    throw new Error(`Failed to generate visual assets: ${error?.message || 'Unknown error'}`);
  }
}

/**
 * Generates streaming chat completion for real-time responses
 * @param {string} userMessage - User input message
 * @param {Function} onChunk - Callback for streaming chunks
 * @returns {Promise<void>}
 */
export async function getStreamingResponse(userMessage, onChunk) {
  try {
    const stream = await openai?.chat?.completions?.create({
      model: 'gpt-4o',
      messages: [
        {
          role: 'system',
          content: 'You are a helpful AI assistant for CinematicBrand Director, specialized in creating director\'s treatments for marketing videos. Provide clear, professional responses about video production, brand analysis, and creative direction.'
        },
        {role: 'user', content: userMessage},
      ],
      stream: true,
      temperature: 0.7,
    });

    for await (const chunk of stream) {
      const content = chunk?.choices?.[0]?.delta?.content || '';
      if (content) {
        onChunk(content);
      }
    }
  } catch (error) {
    console.error('Error in streaming response:', error);
    throw new Error(`Failed to get streaming response: ${error?.message || 'Unknown error'}`);
  }
}

/**
 * Generates a complete director's treatment document content
 * @param {string} brandName - Name of the brand
 * @param {object} companyData - Company analysis data
 * @param {object} storyData - Story outline data
 * @param {object} visualConcepts - Visual concepts data
 * @param {array} assets - Generated visual assets
 * @returns {Promise<object>} Complete treatment data
 */
export async function generateTreatmentDocument(brandName, companyData, storyData, visualConcepts,
    assets) {
  try {
    const response = await openai?.chat?.completions?.create({
      model: 'gpt-4o',
      messages: [
        {
          role: 'system',
          content: `You are a professional creative director creating a comprehensive director's treatment document. 
          Format the content as a professional industry-standard treatment that would be presented to clients and producers.
          Include executive summary, creative concept, production notes, and technical specifications.`
        },
        {
          role: 'user',
          content: `Create a complete director's treatment document for "${brandName}" based on:
          
          Company Analysis: ${JSON.stringify(companyData)}
          Story Outline: ${JSON.stringify(storyData)}
          Visual Concepts: ${JSON.stringify(visualConcepts)}
          Generated Assets: ${assets?.length || 0} visual assets created
          
          Format this as a professional director's treatment with sections for Creative Concept, Story Breakdown, Visual Direction, Production Notes, Budget Considerations, and Timeline.`
        }
      ],
      response_format: {
        type: 'json_schema',
        json_schema: {
          name: 'treatment_document',
          schema: {
            type: 'object',
            properties: {
              title: {type: 'string'},
              executiveSummary: {type: 'string'},
              creativeStrategy: {type: 'string'},
              storyBreakdown: {type: 'string'},
              visualDirection: {type: 'string'},
              productionNotes: {type: 'string'},
              castingNotes: {type: 'string'},
              locationRequirements: {type: 'string'},
              technicalSpecifications: {type: 'string'},
              budgetConsiderations: {type: 'string'},
              timeline: {type: 'string'},
              postProductionNotes: {type: 'string'}
            },
            required: ['title', 'executiveSummary', 'creativeStrategy', 'storyBreakdown',
              'visualDirection', 'productionNotes'],
            additionalProperties: false,
          },
        },
      },
      temperature: 0.6,
    });

    const treatmentContent = JSON.parse(response?.choices?.[0]?.message?.content);

    return {
      ...treatmentContent,
      brandName,
      generatedAt: new Date()?.toISOString(),
      totalAssets: assets?.length || 0,
      documentMetadata: {
        pages: Math.ceil(
            (treatmentContent?.executiveSummary?.length + treatmentContent?.storyBreakdown?.length
                + treatmentContent?.visualDirection?.length) / 2000) || 12,
        size: '2.4 MB',
        format: 'PDF'
      }
    };
  } catch (error) {
    console.error('Error generating treatment document:', error);
    throw new Error(`Failed to generate treatment document: ${error?.message || 'Unknown error'}`);
  }
}

export default {
  analyzeCompany,
  generateStoryOutline,
  generateVisualConcepts,
  generateVisualAssets,
  getStreamingResponse,
  generateTreatmentDocument
};