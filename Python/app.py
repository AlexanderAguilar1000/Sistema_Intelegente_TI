import os
from groq import Groq
from dotenv import load_dotenv
from sentence_transformers import SentenceTransformer
sentences = ["This is an example sentence", "Each sentence is converted"]

model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2')
#embeddings = model.encode(sentences)
#print(embeddings)


load_dotenv()


# Obtengo las variables de entorno del archivo .env
api_key = os.getenv("GROQ_API_KEY")
model=os.getenv("GROQ_MODEL")



if not api_key:
    raise RuntimeError("GROQ_API_KEY no está configurada")

if not model:
    raise RuntimeError("GROQ_MODEL no está configurado")

client = Groq(api_key=api_key)

"""
try:
    respuesta = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "user", "content": "Responde solamente: Conexión exitosa"}
        ],
        max_tokens=50
    )

    print("✅ Conexión con Groq exitosa")
    print("Modelo utilizado:", respuesta.model)
    print("Respuesta:", respuesta.choices[0].message.content)

except Exception as e:
    print("❌ Error al conectar con Groq:")
    print(e)
"""